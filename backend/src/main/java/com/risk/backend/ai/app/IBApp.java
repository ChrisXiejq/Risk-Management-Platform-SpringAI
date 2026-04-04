package com.risk.backend.ai.app;

import com.risk.backend.ai.advisor.BannedWordsAdvisor;
import com.risk.backend.ai.advisor.MyLoggerAdvisor;
import com.risk.backend.ai.agent.GraphTaskAgent;
import com.risk.backend.ai.rag.preretrieval.QueryRewriter;
import com.risk.backend.ai.reflect.ReflectionService;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.stereotype.Component;
import com.risk.backend.ai.graph.PatentGraphRunner;
import com.risk.backend.ai.skills.SkillRegistry;

import java.util.List;
import java.util.Optional;

@Component
@Slf4j
public class IBApp {

    private final ChatModel chatModel;

    @Autowired(required = false)
    private PatentGraphRunner patentGraphRunner;
    private ChatClient chatClient;

    @Resource
    private BannedWordsAdvisor bannedWordsAdvisor;

    private static final String SYSTEM_PROMPT = """
            You are an intelligent assistant for an enterprise security risk assessment platform.
            You help users build a full-loop risk assessment result: risk discovery -> asset assessment -> governance decision.
            At the opening, briefly introduce yourself and explain you can: identify security risks from user input and evidence, assess assets and impacts, and propose governance/migration actions with a practical roadmap.
            During the conversation, you may proactively ask for missing scope information such as organization profile, asset inventory, business context, threat model assumptions, and current controls.
            When you need to recall what the user said earlier or this conversation's history, call the retrieve_history tool with the current conversation_id and a short query (e.g. asset type, business process, control name, or risk category).
            """;

    public IBApp(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /** 默认注入违禁词等 Advisor 后初始化 ChatClient；对话上下文统一由三层记忆 + retrieve_history 提供，不再使用 MessageWindowChatMemory */
    @PostConstruct
    void initChatClient() {
        var advisors = new java.util.ArrayList<Advisor>();
        if (bannedWordsAdvisor != null) {
            advisors.add(bannedWordsAdvisor);
        }
        advisors.add(new MyLoggerAdvisor());
        this.chatClient = ChatClient.builder(chatModel)
                .defaultSystem(SYSTEM_PROMPT)
                .defaultAdvisors(advisors)
                .build();
    }

    @Resource
    private Advisor hybridRagAdvisor;

    @Resource
    @Qualifier("retrievalExpertRagAdvisor")
    private Advisor retrievalExpertRagAdvisor;

    @Resource
    private QueryRewriter queryRewriter;

    // AI 调用工具能力
    @Resource
    private ToolCallback[] allTools;

    @Autowired(required = false)
    @Qualifier("memoryPersistenceAdvisor")
    private Advisor memoryPersistenceAdvisor;

    @Autowired(required = false)
    @Qualifier("agentTraceAdvisor")
    private Advisor agentTraceAdvisor;

    @Autowired(required = false)
    @Qualifier("persistingTraceAdvisor")
    private Advisor persistingTraceAdvisor;

    @Resource
    private ReplanService replanService;

    @Autowired(required = false)
    private SkillRegistry skillRegistry;

    @Autowired(required = false)
    private ReflectionService reflectionService;

    /**
     * 多 Agent 图编排入口，提供给controller的入口
     */
    public String doChatWithMultiAgentOrFull(String message, String chatId) {
        return patentGraphRunner.run(message, chatId);
    }

    private static final String RETRIEVAL_EXPERT_PROMPT = """
            You are the evidence retrieval expert of the enterprise security risk assessment platform.
            Your job is to gather risk indicators and relevant evidence for the user's scenario, including controls, vulnerabilities, threat context, and applicable best practices.
            - Use retrieve_history when you need relevant prior discussion (scope, assets, controls, assumptions).
            - Use searchWeb (web search) when: the user asks for general/conceptual info, or when RAG/knowledge base context is missing or insufficient. Prefer searchWeb for broad or latest-information questions.
            - Use the retrieved RAG context (if provided) as the primary source of factual claims.
            Reply briefly with evidence-focused notes; DO NOT give governance decisions or final risk rankings in this step.
            """;
    private static final String ANALYSIS_EXPERT_PROMPT = """
            You are the asset assessment and impact analysis expert.
            Based on the evidence from the retrieval step and the asset/scope information from the conversation, analyze threats, vulnerabilities, likelihood, impact, and propose a qualitative risk level (if possible).
            Use retrieve_history and RAG context when needed.
            Reply with concise assessment results; DO NOT give governance decisions or implementation roadmaps in this step.
            """;
    private static final String ADVICE_EXPERT_PROMPT = """
            You are the governance decision and mitigation strategy expert.
            Using the assessment results from the analysis step, propose governance actions, control recommendations, and an implementation roadmap.
            Use retrieve_history when you need to respect the assessment scope or prior constraints.
            Reply with actionable mitigation and governance recommendations; avoid inventing evidence that is not supported by the retrieved context.
            """;

    /** 图内专家 Agent 的下一步提示（think/act 循环中）：完成本任务、必要时调用工具、完成后可调用 doTerminate。 */
    private static final String GRAPH_TASK_NEXT_STEP_PROMPT = """
            Complete the current task using the available tools as needed. When you have enough information, summarize briefly for the user.
            Call doTerminate when the task is done.
            """;

    /** Citation 指令：使用检索上下文作答时须标注来源，便于审计与可追溯。 */
    private static final String CITATION_INSTRUCTION = "\n\nWhen answering based on the retrieved context above, you MUST cite the source for each factual claim using the format [1], [2], etc., corresponding to the document order in the context. Do not make up information that is not in the context.";

    /**
     * 按任务类型返回对应 system prompt（Executor 单节点按 task 选 prompt）。
     * 若启用 SkillRegistry，优先从 skills 读取指令，否则回退到内置常量。
     */
    public String getPromptForTask(String task) {
        if (task == null) return resolvePrompt("retrieval");
        String t = task.trim().toLowerCase();
        if (t.contains("retrieval")) return resolvePrompt("retrieval");
        if (t.contains("analysis")) return resolvePrompt("analysis");
        if (t.contains("advice")) return resolvePrompt("advice");
        return resolvePrompt("retrieval");
    }

    private String resolvePrompt(String skillId) {
        String base;
        if (skillRegistry != null) {
            Optional<String> instructions = skillRegistry.getInstructions(skillId);
            if (instructions.isPresent()) base = instructions.get();
            else base = switch (skillId) {
                case "analysis" -> getAnalysisExpertPrompt();
                case "advice" -> getAdviceExpertPrompt();
                default -> getRetrievalExpertPrompt();
            };
        } else {
            base = switch (skillId) {
                case "analysis" -> getAnalysisExpertPrompt();
                case "advice" -> getAdviceExpertPrompt();
                default -> getRetrievalExpertPrompt();
            };
        }
        if ("retrieval".equals(skillId) || "analysis".equals(skillId) || "advice".equals(skillId)) {
            base = base + CITATION_INSTRUCTION;
        }
        return base;
    }

    /** 反思规则后缀：供 doReActForTask 注入到 system prompt，便于专家遵循历史教训。 */
    public String getReflectionSuffix(String chatId) {
        if (reflectionService == null || chatId == null) return "";
        java.util.List<String> rules = reflectionService.getRecentRules(chatId, 5);
        if (rules == null || rules.isEmpty()) return "";
        return "\n\nLessons learned from past steps (follow when relevant): " + String.join("; ", rules);
    }

    /**
     * Executor 单任务 ReAct 执行：使用 IBManus 架构（GraphTaskAgent think→act 多步循环），
     * 按任务类型注入 RAG/记忆/Trace，若 retrieval 上一步不足则注入 searchWeb 补足提示。
     */
    public String doReActForTask(String task, String message, String chatId, List<String> stepResults) {
        String systemPrompt = getPromptForTask(task) + getReflectionSuffix(chatId);
        String effectiveMessage = message;
        // 内部接口下线，调用websearch的兜底
        if (task != null && task.toLowerCase().contains("retrieval") && stepResults != null && !stepResults.isEmpty()) {
            String lastResult = stepResults.get(stepResults.size() - 1);
            if (replanService != null && ReplanService.shouldRetryRetrievalWithWeb(lastResult)) {
                effectiveMessage = "[上一轮证据检索无有效结果（接口失败或无数据），请改用 searchWeb 检索该风险/资产/业务场景或相关公开信息后给出证据要点。] 用户问题："
                        + (message != null ? message : "");
                log.info("[AgentGraph.IBApp] 检索重试：注入 searchWeb 补足提示，原消息长度={}", message != null ? message.length() : 0);
            }
        }
        String rewritten = queryRewriter != null ? queryRewriter.doQueryRewrite(effectiveMessage) : effectiveMessage;
        boolean isRetrievalTask = task != null && task.trim().toLowerCase().contains("retrieval");
        Advisor ragAdvisor = isRetrievalTask && retrievalExpertRagAdvisor != null
                ? retrievalExpertRagAdvisor : hybridRagAdvisor;
        GraphTaskAgent agent = new GraphTaskAgent(
                allTools, chatModel, systemPrompt, GRAPH_TASK_NEXT_STEP_PROMPT, chatId,
                ragAdvisor, memoryPersistenceAdvisor, agentTraceAdvisor, persistingTraceAdvisor);
        String out = agent.run(rewritten);
        String expertName = systemPrompt.contains("retrieval") ? "Retrieval" : systemPrompt.contains("analysis") ? "Analysis" : "Advice";
        log.info("[AgentGraph.IBApp][doReActForTask] 专家调用(IBManus) expert={} messageLength={} responseLength={}",
                expertName, effectiveMessage != null ? effectiveMessage.length() : 0, out != null ? out.length() : 0);
        return out != null ? out : "";
    }

    /** 简单问候或“介绍自己”类短句，无需走检索专家，直接 synthesize 即可 */
    private static boolean isSimpleGreetingOrIntro(String userMessage) {
        if (userMessage == null) return false;
        String s = userMessage.trim();
        if (s.length() > 50) return false;
        String lower = s.toLowerCase();
        return lower.contains("你好") || lower.contains("嗨") || lower.contains("hi ") || lower.equals("hi")
                || lower.contains("介绍自己") || lower.contains("自我介绍") || lower.contains("你是谁")
                || lower.contains("introduce yourself");
    }

    private static String abbreviate(String s, int maxLen) {
        if (s == null) return "null";
        s = s.trim();
        if (s.length() <= maxLen) return s;
        return s.substring(0, maxLen) + "...";
    }

    /**
     * 综合节点：根据已有 stepResults 与用户问题，生成最终回复。
     */
    public String synthesizeAnswer(String userMessage, List<String> stepResults, String chatId) {
        String context = stepResults == null || stepResults.isEmpty()
                ? "No prior expert outputs."
                : String.join("\n---\n", stepResults);
        String prompt = """
                You are the enterprise security risk assessment platform assistant.
                Below are expert outputs from retrieval/analysis/advice agents.
                Provide a structured final risk assessment report:
                1) Risk discovery (what risks are identified and why)
                2) Asset assessment (asset scope, likely threats/vulnerabilities, impact & likelihood, qualitative risk level)
                3) Governance decision (recommended controls, governance actions, and an implementation roadmap)
                Keep it concise and actionable. Do not repeat long raw data; summarize and highlight next steps and open uncertainties.
                User question: %s
                Expert outputs:
                %s
                """.formatted(userMessage, context);
        ChatResponse resp = chatClient.prompt()
                .user(prompt)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .call()
                .chatResponse();
        String content = resp.getResult().getOutput().getText();
        log.info("[AgentGraph.IBApp][synthesizeAnswer] 综合节点 stepResultsCount={} finalAnswerLength={}",
                stepResults != null ? stepResults.size() : 0, content != null ? content.length() : 0);
        return content != null ? content : "";
    }

    // ========== P&E：规划与重规划 ==========

    private static final String PLAN_PROMPT = """
            You are the planner for an enterprise security risk assessment agent.
            Given the user message and optional prior step results, output a comma-separated list of steps to execute.
            Each step is exactly one of: retrieval, analysis, advice, synthesize.
            - retrieval: discover risks and gather evidence (from RAG/knowledge base, or searchWeb when needed).
            - analysis: assess assets and analyze impact/likelihood to produce qualitative risk levels.
            - advice: make governance decisions and recommend mitigation controls + roadmap.
            - synthesize: generate the final risk assessment report and end.
              Use synthesize when the user only greets/asks who you are/thanks, or when we already have enough scope and evidence.
            Rules: Use minimal steps. For simple greeting or "who are you" reply only: synthesize. For "discover risks then assess" reply: retrieval,analysis,synthesize. For full pipeline: retrieval,analysis,advice,synthesize.
            Reply with only the comma-separated list, e.g. retrieval,analysis,synthesize or synthesize.
            User message: %s
            Prior step results (if any): %s
            """;

    /**
     * P&E 初始规划：根据用户问题（及已有 stepResults、chatId 用于注入反思规则）生成执行计划。
     */
    public List<String> createPlan(String userMessage, List<String> stepResults, String chatId) {
        if (userMessage == null) userMessage = "";
        if (stepResults == null) stepResults = List.of();
        if (isSimpleGreetingOrIntro(userMessage)) {
            log.info("[AgentGraph.IBApp][createPlan] 规划 识别为简单问候，直接 synthesize");
            return List.of("synthesize");
        }
        String prior = stepResults.isEmpty() ? "None" : String.join("\n---\n", stepResults);
        String lessons = "";
        if (reflectionService != null && chatId != null) {
            List<String> rules = reflectionService.getRecentRules(chatId, 5);
            if (rules != null && !rules.isEmpty()) {
                lessons = "\nPrior lessons (consider when planning): " + String.join("; ", rules);
            }
        }
        String prompt = PLAN_PROMPT.formatted(userMessage, prior) + lessons;
        ChatResponse resp = chatClient.prompt().user(prompt).call().chatResponse();
        String raw = resp.getResult().getOutput().getText();
        List<String> plan = parsePlan(raw);
        log.info("[AgentGraph.IBApp][createPlan] 规划 userMessage(preview)= {} -> plan={}", abbreviate(userMessage, 50), plan);
        return plan.isEmpty() ? List.of("synthesize") : plan;
    }

    /**
     * 检查上一步执行结果是否表明「环境变化」，需动态更新剩余任务。
     */
    public boolean checkEnvironmentChange(String lastStepResult, List<String> remainingTasks, String userMessage) {
        if (lastStepResult == null || lastStepResult.isBlank()) return false;
        String lower = lastStepResult.trim().toLowerCase();
        if (lower.contains("风险已缓解") || lower.contains("已降低风险") || lower.contains("已合规") || lower.contains("控制已实施")
                || lower.contains("scope changed") || lower.contains("scope is changed") || lower.contains("out of scope")) return true;
        // 上一步结果已明确不足/缺信息时，不视为环境变化，交给 RePlan 的不足分支处理，避免误触发循环。
        if (ReplanService.isResultInsufficient(lastStepResult)) return false;
        if (lower.contains("no relevant asset") || lower.contains("no asset") || lower.contains("no data") || lower.contains("insufficient evidence")
                || lower.contains("contradiction") || lower.contains("conflict")) return true;
        // 为了避免该判定再次触发 LLM 调用造成 503/超时，默认采用规则优先；无法判定时保守返回 false。
        log.info("[AgentGraph.IBApp][checkEnvironmentChange] 规则判定未命中，降级为 false（remainingSize={}）", remainingTasks != null ? remainingTasks.size() : 0);
        return false;
    }

    private static List<String> parsePlan(String raw) {
        if (raw == null) return List.of();
        List<String> out = new java.util.ArrayList<>();
        for (String s : raw.trim().toLowerCase().split("[,，\\s]+")) {
            String t = s.trim();
            if (t.isEmpty()) continue;
            if (t.contains("retrieval")) out.add("retrieval");
            else if (t.contains("analysis")) out.add("analysis");
            else if (t.contains("advice")) out.add("advice");
            else if (t.contains("synthesize") || t.contains("end")) out.add("synthesize");
        }
        if (!out.isEmpty() && !"synthesize".equals(out.get(out.size() - 1))) {
            out.add("synthesize");
        }
        return out;
    }

    public String getRetrievalExpertPrompt() { return RETRIEVAL_EXPERT_PROMPT; }
    public String getAnalysisExpertPrompt() { return ANALYSIS_EXPERT_PROMPT; }
    public String getAdviceExpertPrompt() { return ADVICE_EXPERT_PROMPT; }

    /**
     * 和 RAG 知识库进行对话
     * @param message
     * @param chatId
     * @return
     * 仅用来跑RAGAS测试，不对外提供controller
     */
    public String doChatWithRag(String message, String chatId) {
        ChatResponse chatResponse = chatClient
                .prompt()
                .user(message)
                .advisors(spec -> spec.param(ChatMemory.CONVERSATION_ID, chatId))
                .advisors(hybridRagAdvisor)
                .call()
                .chatResponse();
        String content = chatResponse.getResult().getOutput().getText();
        log.info("content: {}", content);
        return content;
    }
}
