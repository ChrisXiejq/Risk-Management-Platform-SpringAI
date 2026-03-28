# 企业安全风险评估智能 Agent 平台 — 面试准备手册

本文档汇总**行业常见实现方式**、**库表与索引设计**、**分布式/高并发/缓存/一致性/安全**注意点，以及**性能与错误率**口径，便于对照简历与代码自查。技术栈假设：**Spring Boot + Spring AI + LangGraph4j + MySQL + PostgreSQL(PgVector)**。

---

## 目录

1. [分层架构总览](#1-分层架构总览)
2. [Agent 图编排与状态](#2-agent-图编排与状态)
3. [RePlan 与 Reflect](#3-replan-与-reflect)
4. [Agent Skills](#4-agent-skills)
5. [三层记忆架构](#5-三层记忆架构)
6. [Agent Tracer 可审计](#6-agent-tracer-可审计)
7. [集中式工具注册与 MCP](#7-集中式工具注册与-mcp)
8. [分布式、高并发、缓存与安全清单](#8-分布式高并发缓存与安全清单)
9. [Agent 可量化指标体系](#9-agent-可量化指标体系)
10. [与仓库现有文档的对照](#10-与仓库现有文档的对照)
11. [附录 A：MySQL 完整 DDL](#附录-amysql-完整-ddl)
12. [附录 B：PostgreSQL + PgVector DDL](#附录-bpostgresql--pgvector-ddl)

---

## 1. 分层架构总览

| 层次 | 常见职责 | 简历对应 |
|------|----------|----------|
| 编排层 | LangGraph4j 状态图、节点、边、检查点 | P&E、Planner、Executor、RePlanner、CheckResult、AfterExpert |
| 推理层 | LLM、ReAct、工具调用协议 | ReAct、`maxSteps` / `max_react_rounds` |
| 检索层 | RAG（向量 + 关键词 + 重排）、Web Search | PgVector、BM25/RRF/rerank |
| 记忆层 | Working / Experiential / Semantic | importance、摘要、decay、NLI |
| 治理层 | Trace、审计、配额、脱敏 | Spring AI Advisor + 落库 |
| 集成层 | Tool Registry、MCP/HTTP | 单例 + Bean、可选独立部署 |

---

## 2. Agent 图编排与状态

### 2.1 状态（Graph State）常见字段

- **会话与输入**：`session_id`、`tenant_id`、`user_id`、`user_message`
- **规划**：`plan`（任务列表 JSON）、`current_task_id`、`plan_version`
- **执行**：`step_count`、`max_steps`、`next_node`、`step_results[]`
- **中间产物**：`artifacts`（检索片段 ID、工具原始 JSON）
- **失败与重试**：`error`、`retry_count`、`termination_reason`

### 2.2 Planner / Executor / 兜底

- **Planner**：输出结构化任务（每项含 `id`、`type`、`inputs`、`success_criteria`、`depends_on`）。
- **Executor**：子任务内 **ReAct**：思考 → 工具 → 观察 → …，直到满足终止条件或达到最大轮次。
- **CheckResult**：校验输出是否满足成功条件、schema、与证据一致性。
- **RePlanner**：失败或冲突时改 plan（增量优先，全量兜底）。
- **AfterExpert**：去重、引用规范化、写入 `step_results`。

### 2.3 分布式与一致性要点

- **会话亲和**：同一 `session_id` 尽量避免并行两路改同一状态；需要时用分布式锁（Redis Redisson / DB `SELECT … FOR UPDATE`）或串行队列。
- **幂等**：客户端 `request_id`；节点重复执行用 `idempotency_key` 去重。
- **检查点（Checkpoint）**：状态持久化到 DB/Redis，支持崩溃恢复（仅内存易丢上下文）。

### 2.4 与仓库 `agent-graph-execution-flow.md` 的对照

- Router + 多轮 Expert + `maxSteps` 后强制 Synthesize = **防死循环 + 强制收敛**。
- 路由应注入 **`stepResults` 摘要、上一步节点类型**，避免同一意图被重复调用导致延迟与成本飙升。

---

## 3. RePlan 与 Reflect

### 3.1 RePlan 常见触发

| 触发 | 处理策略 |
|------|----------|
| 工具超时/5xx | 退避重试 → 换备用工具 → RePlan |
| RAG 空/low score | 查询改写、扩域、Web Search |
| 专家结论冲突 | 仲裁子任务或 RePlan 增加「证据对齐」 |
| 步数/token 预算耗尽 | 降级 Synthesize，并标明证据不足 |

### 3.2 Reflect 常见形态

- **输入**：预期输出、实际输出、工具与检索引用。
- **输出**：JSON 归因（权限限制、检索参数漂移、逻辑链路断裂等）+ `suggested_rule`、`severity`。
- **写入**：可先 **staging**，高严重度或人工审核后再并入生产策略库。

### 3.3 表设计要点（MySQL）

- `agent_plan`：`session_id` + `plan_version` 唯一，支持版本链。
- `agent_replan_event`：记录触发原因与前后版本，便于审计与指标统计。
- `agent_reflect_report`：`report_json` 存结构化复盘，便于后续注入 prompt。

详见 [附录 A](#附录-amysql-完整-ddl)。

---

## 4. Agent Skills

### 4.1 常见实现

- 启动时扫描 `skills/**/*.md` 或清单 JSON，构建 `skill_id → { metadata, content_path }`。
- **Plan 阶段**：只加载元数据（名称、简介、`when_to_use`、关键词），控制 token。
- **Execute 阶段**：按需加载完整 SOP。

### 4.2 安全

- 路径规范化，防路径遍历；`content_hash` 防篡改。
- 按租户/角色过滤可加载技能。

表 `agent_skill_registry` 见附录 A。

---

## 5. 三层记忆架构

### 5.1 Working Memory

- 滑动窗口 + **importance**（规则或轻量打分）；低价值寒暄可丢弃。
- 建议 **MySQL** 按 `session_id` + `seq` 有序存储，便于截断与审计。

### 5.2 Experiential Memory

- 窗口溢出 → **递归摘要** LLM → 写入存储；**decay_weight** 作元数据（可离线任务递减）。
- **向量检索**适合放在 **PostgreSQL + pgvector**（与 RAG 同栈或分表），MySQL 仅存外链 `pg_row_id` 或业务 `memory_uuid`。

### 5.3 Semantic Memory

- 新事实 → topK 相似检索 → **NLI/LLM** 判定矛盾 → 冲突则新版本或 `supersedes_id` 链。
- 表 `semantic_memory_fact` 建议 `user_id` + `status` 索引，便于拉取「当前有效事实」。

### 5.4 拉取方式

- 通过 **Tool** 按需 `search_memory(scope, query, top_k)`，避免全量注入上下文。

### 5.5 缓存与一致性

- Redis 缓存热点 session：**key 必须含 `tenant_id` + `user_id`**，防串租户。
- 摘要任务可用 **Outbox** 表保证「至少投递一次」，避免丢摘要。

---

## 6. Agent Tracer 可审计

### 6.1 在现有 `agent_trace` 上建议扩展

现有仓库：`backend/docs/schema-agent-trace.sql`。

建议增加（按合规强度选用）：

- `tenant_id`、`user_id`：多租户与主体
- `trace_id` / `span_id`：对齐日志与 APM
- `node_name`：图节点名
- `latency_ms`、`model_name`：性能与成本
- `error_code`：失败率统计
- `pii_redacted` 或脱敏在写入前完成

### 6.2 索引

- `(tenant_id, session_id, created_at)`：租户下按会话拉时间线
- `created_at`：归档与分区

### 6.3 写入模式

- **强审计**：同步写库或 WAL。
- **低延迟优先**：异步队列批量写，接受极低概率丢失时需监控补偿任务。

---

## 7. 集中式工具注册与 MCP

### 7.1 Registry

- Spring 单例 + `Map<String, ToolDefinition>`；支持 `enabled`、超时、熔断（连续失败打开）。

### 7.2 MCP / HTTP 工具

- TLS；内网 **mTLS**；工具级 API Key，最小权限。
- **超时** 2–5s 常见；**重试** 0–2 次指数退避。
- **限流**：按租户/用户 QPS，防止刷爆 LLM 与下游。

### 7.3 缓存安全

- 只读资产类可缓存；**TTL + 版本号**；敏感字段慎存 Redis。

---

## 8. 分布式、高并发、缓存与安全清单

| 主题 | 建议 |
|------|------|
| 多租户 | 所有查询带 `tenant_id`；禁止仅靠 `session_id` 猜测 |
| IDOR | `session_id` 使用不可枚举 ID；服务端校验归属 |
| 并发写状态 | 乐观锁 `plan_version` 或分布式锁 |
| 缓存 | Key 含租户+用户；禁止跨租户复用 |
| 脱敏 | Trace preview 截断 + 规则脱敏 |
| 密钥 | 环境变量/密钥管理；禁止写入 Trace |
| 一致性 | 关键事件 Outbox；向量与关系库最终一致可接受时显式文档化 |

---

## 9. Agent 可量化指标体系

本节给出 Agent 平台常用的**可量化结果指标**：每个指标包含**定义/公式**、**怎样算出来**、**数据从哪里来**。  
**重要**：下表中的「参考区间」来自常见线上观测与公开实践讨论，**不是行业标准阈值**；你应在简历/答辩中用**自己环境**在固定时间窗口内统计出的分位数与比例替换。

### 9.1 统计口径（先约定再算数）

| 约定项 | 说明 |
|--------|------|
| **时间窗口** | 如最近 7 天 / 30 天、仅工作日、仅生产环境 `prod` |
| **主体** | 按 `tenant_id`、按产品线、或全平台；避免混测与生产 |
| **会话粒度** | 一次用户请求对应一个 `session_id`（或 `trace_id` 根） |
| **成功定义（业务）** | 需写清：例如「返回 HTTP 200 且未抛未捕获异常」与「业务侧 CheckResult 通过」是否为同一指标；面试建议拆开 **工程成功率** 与 **任务成功率** |
| **分母** | 「每会话」「每轮用户消息」「每次图运行」「每次工具调用」必须标明，否则比例不可比 |

### 9.2 指标一览：定义、计算方式、数据来源

| 指标 | 定义 / 公式 | 怎样得到（计算步骤） | 数据来源（优先级从高到低） |
|------|----------------|----------------------|----------------------------|
| **端到端成功率（会话级）** | \( \frac{\text{成功会话数}}{\text{总会话数}} \) | 窗口内每个 `session_id` 打标：成功=满足约定（如正常结束节点+无 `error_code`）；汇总比例 | **应用日志**（结构化 JSON，含 `session_id`、`outcome`）；**`agent_trace`** 聚合（见 9.4）；网关/负载均衡访问日志（仅 HTTP 层成功，不含业务） |
| **端到端失败率** | \( 1 - \text{成功率} \) 或单独统计显式失败 | 与上共用分母；失败=超时、5xx、业务 `CheckResult` 不通过、用户中断 | 同上；**APM**（错误类型） |
| **工程可用率 / 错误率** | \( \frac{\text{HTTP 5xx 或未处理异常次数}}{\text{请求次数}} \) | 按 API 维度聚合 | **网关/Nginx**、**Spring Boot Actuator**、**APM**（如 OpenTelemetry `http.server.duration` + `status_code`） |
| **链路完成率（图跑完率）** | \( \frac{\text{到达终止节点（如 End/Synthesize）的会话数}}{\text{启动图执行的会话数}} \) | 每个 `session_id` 是否出现 `node_name=end` 或等价终止事件且无异常 | **`agent_trace.node_name`**、编排框架的 **checkpoint/结束事件**、应用日志 `graph.completed` |
| **提前终止率 / 触顶率（maxSteps）** | \( \frac{\text{因 step\_count≥maxSteps 或预算触顶而结束的会话数}}{\text{总会话数}} \) | 解析终止原因字段或日志关键字 `max_steps_reached` | **图状态持久化**、**`agent_trace.metadata_json`**（建议写入 `termination_reason`）、`agent-graph-execution-flow.md` 同类日志 |
| **首 Token 延迟 TTFT** | 从收到请求到**首个流式 chunk** 的时间 | 客户端或服务端在首个 chunk 打点 \(t_1 - t_0\) | **服务端流式响应拦截器**；前端 **RUM**；LLM SDK 回调时间戳 |
| **端到端延迟（E2E）** | 从请求进入到**最终完整回复**的时间 | 单次会话 \(t_\text{end}-t_\text{start}\)；报告 **P50/P95/P99** | **网关** `request_time`；**APM span**（根 span duration）；**`agent_session`/`agent_trace` 首尾 `created_at` 差**（粗粒度） |
| **节点级延迟** | 单节点耗时 | 节点入口/出口时间差；或对 `agent_trace` 按 `node_name` 分组取 `latency_ms` 的分位数 | **`agent_trace.latency_ms`**（需在 Advisor 或节点切面写入）；**APM 子 span**（每个节点一个 span） |
| **LLM 调用延迟** | 单次 Chat/Completion 耗时 | LLM 客户端包装计时；分模型、分路由 | **Spring AI / ChatClient 拦截**；提供商 **Dashboard**（OpenAI/Azure 等） |
| **LLM 调用失败率** | \( \frac{\text{失败次数}}{\text{LLM 调用次数}} \) | 失败=超时、429、5xx、内容过滤错误等（按团队定义） | **应用日志** `llm.error`；**提供商 API 响应码**；**APM** external call 错误率 |
| **LLM 重试后失败率** | 在最大重试用尽后仍失败的比例 | 与上类似，分母为「需调用 LLM 的请求」，分子为「重试后仍失败」 | 同上 + **重试中间件**计数 |
| **工具调用次数（每会话）** | 平均/ P95：单会话内 tool call 次数 | 对 `tool_calls_json` 数组长度按会话求和再聚合 | **`agent_trace.tool_calls_json`**（每步一条记录时需按 `session_id` 汇总）；结构化日志 `tool.invoke` |
| **工具使用率（会话级）** | \( \frac{\text{至少发生 1 次工具调用的会话数}}{\text{总会话数}} \) | 会话内是否存在非空 `tool_calls_json` 或等价标记 | **`agent_trace`** 聚合；日志 |
| **工具覆盖率（注册表意义）** | \( \frac{\text{某类业务场景下实际被调用过的工具种类数}}{\text{Registry 中已启用且期望覆盖的工具种类数}} \) | 按场景打标签（如「风险评估-资产」），统计窗口内出现过的 `tool_name` 集合与期望集合之比 | **Registry 配置** + **`tool_calls_json` 中去重 `tool_name`**；需维护「期望工具清单」配置 |
| **工具覆盖率（计划命中意义，可选）** | \( \frac{\text{Planner 计划要用的工具中被实际执行的比例}}{\text{计划中的工具数}} \) | 对比 `agent_plan.plan_json` 与 trace 中实际调用 | **`agent_plan`** + **`agent_trace`** |
| **工具错误率（调用级）** | \( \frac{\text{返回错误/异常的工具调用次数}}{\text{工具调用总次数}} \) | 每次 tool invoke 打标 `ok/error`；HTTP 非 2xx、超时、业务错误码计入分子 | **工具适配器**统一捕获；**`tool_calls_json` 内嵌 `status`**（需在实现中写入）；MCP 客户端日志 |
| **工具超时率** | \( \frac{\text{超时次数}}{\text{工具调用次数}} \) | 分子为 `TimeoutException` 或状态 `timeout` | **客户端超时配置** + 日志；APM |
| **单工具错误率（按名称）** | 按 `tool_name` 分组的上式 | Group by `tool_name` | **`tool_calls_json`** 解析；Metrics label `tool_name` |
| **RePlan 触发率（会话级）** | \( \frac{\text{发生过至少一次 RePlan 的会话数}}{\text{总会话数}} \) | 统计 `agent_replan_event` 中 distinct `session_id` | **`agent_replan_event`**（权威）；或 **`agent_trace`** 中 `step_type=replan` |
| **RePlan 次数（每会话）** | 平均或 P95 | 按 `session_id` count replan 事件 | **`agent_replan_event`** |
| **检索空结果率（RAG）** | \( \frac{\text{topK 为 0 或 score 低于阈值的检索次数}}{\text{检索总次数}} \) | 每次 RAG 请求记录 `hit_count`/`max_score` | **检索服务日志**；**`agent_trace.retrieval_docs_json` 为空** 的比例（粗）；向量库查询日志 |
| **检索延迟 P95** | 单次向量+混合检索耗时 | 检索组件计时 | **PgVector/检索微服务** metrics；APM internal span |
| **Reflect 触发率 / 产出率** | 会话级或任务级占比 | 统计 `agent_reflect_report` 条数 / 会话数 | **`agent_reflect_report`** |
| **Token 消耗（总量/每会话）** | `sum(prompt_tokens+completion_tokens)` | 从 LLM 响应或计费接口汇总 | **提供商 Billing API**；**`metadata_json.token_usage`**（Spring AI 常可拿到）；成本看板 |
| **Trace 写入成功率** | \( \frac{\text{成功写入的 trace 条数}}{\text{预期写入条数}} \) | Advisor 写库返回成功 / 异步队列消费成功 | **DB 写入结果**；**死信队列**；对账任务 |
| **脱敏合规率（可选）** | 抽样审计中 `pii_redacted=1` 或无明文敏感信息的占比 | 离线扫描 + 人工抽检 | **`agent_trace.pii_redacted`**；安全扫描任务 |

### 9.3 数据来源分层（面试可一句话说明）

| 层级 | 典型系统 | 适合支撑的指标 |
|------|----------|------------------|
| **客户端** | Web/Mobile RUM | TTFT、E2E（含网络）、交互失败 |
| **网关** | Nginx / API Gateway | QPS、5xx、**粗粒度 E2E**（TTFB 到网关） |
| **应用** | Spring Boot + 结构化日志 | 业务成功/失败、节点名、工具名、session 维度 |
| **Advisor / Trace 表** | `agent_trace`、RePlan/Reflect 表 | 链路完成、工具与检索明细、审计级复盘 |
| **APM / OTel** | Jaeger / Datadog / 云 APM | 延迟分位数、依赖服务错误率、分布式链路 |
| **LLM 提供商** | OpenAI/Azure 控制台 | 限流 429、区域错误率、官方延迟统计 |
| **向量库 / MCP** | PostgreSQL、MCP Server 日志 | 检索延迟、工具侧错误 |

### 9.4 与本项目表结构的字段映射（实现时对齐）

| 指标 | 建议在落库/日志中的字段 |
|------|-------------------------|
| 会话与租户 | `tenant_id`、`user_id`、`session_id` |
| 节点与步骤 | `node_name`、`step_type`、`plan_version` |
| 延迟 | `latency_ms`（节点级）；根 span 或首尾时间戳（E2E） |
| 工具 | `tool_calls_json`：每项含 `tool_name`、`status`、`duration_ms`、`error` |
| 检索 | `retrieval_docs_json`：`doc_ids`、`scores`、`hit_count` |
| 失败 | `error_code`、HTTP 状态、LLM `finish_reason`（可放 `metadata_json`） |
| 终止原因 | `metadata_json.termination_reason`（如 `completed` / `max_steps` / `user_abort`） |

### 9.5 SLO 经验参考区间（非标准，需用自有数据替换）

以下仅作**答辩时的数量级参考**，避免被追问「数字哪来的」时答「行业标准」——应改口为「我们线上/压测在某某窗口统计得到 P95=…」。

| 指标 | 说明 | 经验量级（替换为自有统计） |
|------|------|----------------------------|
| E2E P95 | 端到端完整回复 | 简单问答常见 **5–15s**；多步 Agent **30–120s** |
| TTFT P95 | 首 token | 流式常见 **0.5–3s**（视模型与排队） |
| LLM 失败率（单次调用） | 未重试 | 优质线路常见 **小于 1%**；波动时 **1–3%** |
| 工具错误率（调用级） | 含超时 | 常见 **2–8%**；稳定后目标可压到 **小于 5%** |
| RAG 空结果率 | 按检索次数 | 语料差时 **10–30%**；优化后 **小于 10–15%** |
| RePlan 会话占比 | 至少一次 RePlan | 复杂场景 **5–25%** 不等，需结合业务 |
| Trace 异步写入额外延迟 | 不占用户关键路径 | 常见 **小于 50ms**（异步） |

### 9.6 面试表述示例（可信、可验证）

「成功率、失败率、E2E P95、工具错误率，我们是在 **生产/预发** 取 **最近 7 天**、按 **`tenant_id` 维度** 汇总的：成功定义是 **HTTP 200 + 图到达 Synthesize 且无未处理异常**；工具错误率的分母是 **trace 里解析出的每次 tool invoke**，分子是 **status=error 或日志里 Timeout**。数据来源主要是 **`agent_trace` + 结构化日志**，延迟以 **APM 根 span** 为准对齐网关时间。」

---

## 10. 与仓库现有文档的对照

| 文档 | 内容 |
|------|------|
| `agent-graph-execution-flow.md` | Router、Expert、`maxSteps`、Synthesize、路由与 `stepResults` 优化、Advisor 重复注册问题 |
| `schema-agent-trace.sql` | `agent_trace` 基础表 |

---

## 附录 A：MySQL 完整 DDL

以下适用于 **MySQL 8.0+**（`JSON`、`TIMESTAMP(3)`）。执行前请根据实际字符集、分库分表策略调整。

```sql
-- =============================================================================
-- 企业安全风险评估智能 Agent 平台 — MySQL DDL（面试/设计参考）
-- 字符集建议：utf8mb4；引擎：InnoDB
-- =============================================================================

SET NAMES utf8mb4;

-- -----------------------------------------------------------------------------
-- 1) Agent 审计 Trace（在现有 agent_trace 上扩展）
-- 原表见 schema-agent-trace.sql；以下为增强版，可新建 migration 逐步加列
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS agent_trace (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL DEFAULT '',
    user_id VARCHAR(64) NOT NULL DEFAULT '',
    session_id VARCHAR(255) NOT NULL,
    trace_id VARCHAR(64) DEFAULT NULL COMMENT '分布式追踪 ID',
    span_id VARCHAR(64) DEFAULT NULL,
    node_name VARCHAR(128) DEFAULT NULL COMMENT '图节点名，如 Planner/RetrievalExpert',
    step_type VARCHAR(64) DEFAULT NULL,
    plan_version INT DEFAULT NULL,
    model_name VARCHAR(128) DEFAULT NULL,
    latency_ms INT DEFAULT NULL,
    error_code VARCHAR(64) DEFAULT NULL,
    request_preview TEXT,
    response_preview TEXT,
    tool_calls_json JSON DEFAULT NULL,
    retrieval_docs_json JSON DEFAULT NULL,
    metadata_json JSON DEFAULT NULL COMMENT '扩展字段，如 token 用量',
    pii_redacted TINYINT(1) NOT NULL DEFAULT 0,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_tenant_session_time (tenant_id, session_id, created_at),
    KEY idx_session_id (session_id),
    KEY idx_created_at (created_at),
    KEY idx_trace_id (trace_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Agent 执行链路审计，支持按租户/会话/时间查询';

-- -----------------------------------------------------------------------------
-- 2) 规划版本（Plan-and-Execute）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS agent_plan (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    plan_version INT NOT NULL DEFAULT 1,
    plan_json JSON NOT NULL COMMENT '任务列表：id/type/inputs/success_criteria/depends_on',
    status ENUM('draft','active','superseded') NOT NULL DEFAULT 'active',
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_session_plan_version (tenant_id, session_id, plan_version),
    KEY idx_session_status (tenant_id, session_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 3) 重规划事件（可审计、可统计触发率）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS agent_replan_event (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    from_plan_version INT NOT NULL,
    to_plan_version INT NOT NULL,
    trigger_type ENUM(
        'tool_fail',
        'retrieval_empty',
        'expert_conflict',
        'budget_exceeded',
        'manual',
        'other'
    ) NOT NULL,
    reason_summary VARCHAR(512) DEFAULT NULL,
    detail_json JSON DEFAULT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_tenant_session_time (tenant_id, session_id, created_at),
    KEY idx_trigger (trigger_type, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 4) Reflect 复盘报告
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS agent_reflect_report (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    task_id VARCHAR(64) DEFAULT NULL COMMENT '对应 plan 中任务 id',
    node_name VARCHAR(128) DEFAULT NULL,
    report_json JSON NOT NULL COMMENT '归因、建议规则、severity 等',
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_tenant_session (tenant_id, session_id),
    KEY idx_created (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 5) Agent Skills 注册（可选：文件为主时也可只用代码注册）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS agent_skill_registry (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    skill_id VARCHAR(128) NOT NULL,
    version VARCHAR(32) NOT NULL,
    metadata_json JSON NOT NULL COMMENT 'name/desc/tags/when_to_use/keywords',
    content_hash CHAR(64) NOT NULL COMMENT 'SHA-256 防篡改',
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_skill_version (skill_id, version),
    KEY idx_enabled (enabled)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 6) Working Memory（滑动窗口 + importance）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS working_memory_turn (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    seq INT NOT NULL COMMENT '会话内单调递增序号',
    role ENUM('user','assistant','system','tool') NOT NULL,
    content MEDIUMTEXT,
    importance_score DECIMAL(6,4) DEFAULT NULL COMMENT '0~1 或自定义刻度',
    token_estimate INT DEFAULT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_session_seq (tenant_id, session_id, seq),
    KEY idx_session_time (tenant_id, session_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 7) Semantic Memory（长期事实/偏好；向量可只在 PG 存，此处存文本与版本链）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS semantic_memory_fact (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    fact_text TEXT NOT NULL,
    status ENUM('active','superseded') NOT NULL DEFAULT 'active',
    supersedes_id BIGINT DEFAULT NULL,
    confidence DECIMAL(6,4) DEFAULT NULL,
    source ENUM('user_explicit','inferred','reflect','import') DEFAULT 'inferred',
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    KEY idx_user_active (tenant_id, user_id, status),
    KEY idx_supersedes (supersedes_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 8) 记忆摘要 Outbox（Working 溢出 → 异步摘要 → Experiential）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS memory_summary_outbox (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    payload_json JSON NOT NULL COMMENT '待摘要的 turn id 列表或文本快照',
    status ENUM('pending','processing','done','failed') NOT NULL DEFAULT 'pending',
    retry_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    KEY idx_status_time (status, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- -----------------------------------------------------------------------------
-- 9) 可选：会话元数据（检查点外键、图运行状态）
-- -----------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS agent_session (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    title VARCHAR(256) DEFAULT NULL,
    state_json JSON DEFAULT NULL COMMENT 'LangGraph checkpoint 摘要或外链键',
    active_plan_version INT DEFAULT NULL,
    created_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3),
    updated_at TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3),
    UNIQUE KEY uk_tenant_session (tenant_id, session_id),
    KEY idx_user_updated (tenant_id, user_id, updated_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

### 分区与归档（大表 `agent_trace`）

数据量极大时，可按 `created_at` **按月 RANGE 分区**（MySQL 8），或冷热分离：热数据保留 30–90 天，历史归档对象存储/离线数仓。分区 DDL 与运维策略依实际 DBA 规范编写，此处不展开固定脚本。

---

## 附录 B：PostgreSQL + PgVector DDL

Experiential（及可选 Semantic 向量）与 RAG 共用 **pgvector** 时，使用下列表结构。`memory_uuid` 可与 MySQL 侧 `semantic_memory_fact.id` 或独立 UUID 对齐。

```sql
-- =============================================================================
-- PostgreSQL 15+ 示例；需 CREATE EXTENSION vector;
-- embedding 维度按所用模型修改（如 text-embedding-3-large 为 3072）
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS vector;

-- Experiential Memory：摘要 + 向量 + decay 元数据
CREATE TABLE IF NOT EXISTS experiential_memory (
    id BIGSERIAL PRIMARY KEY,
    memory_uuid UUID NOT NULL DEFAULT gen_random_uuid(),
    tenant_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64),
    summary_text TEXT NOT NULL,
    metadata_json JSONB NOT NULL DEFAULT '{}',
    decay_weight NUMERIC(8,4) NOT NULL DEFAULT 1.0000,
    embedding vector(1536) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    last_accessed_at TIMESTAMPTZ,
    CONSTRAINT uq_experiential_uuid UNIQUE (memory_uuid)
);

CREATE INDEX IF NOT EXISTS idx_exp_tenant_user_time
    ON experiential_memory (tenant_id, user_id, created_at DESC);

-- 向量索引：数据量较小时可先建普通索引或不建 ANN，待行数上万再调 ivfflat/hnsw
-- lists / m / ef_construction 需按数据量与召回要求调参
CREATE INDEX IF NOT EXISTS idx_exp_embedding_ivfflat
    ON experiential_memory
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);

-- 可选：HNSW（pgvector 0.5+，需确认实例支持）
-- CREATE INDEX IF NOT EXISTS idx_exp_embedding_hnsw
--     ON experiential_memory
--     USING hnsw (embedding vector_cosine_ops);

-- 可选：RAG 文档块（若与业务库同实例）
CREATE TABLE IF NOT EXISTS rag_document_chunk (
    id BIGSERIAL PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    doc_id VARCHAR(128) NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1536) NOT NULL,
    metadata_json JSONB NOT NULL DEFAULT '{}',
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE (tenant_id, doc_id, chunk_index)
);

CREATE INDEX IF NOT EXISTS idx_rag_tenant_doc ON rag_document_chunk (tenant_id, doc_id);
CREATE INDEX IF NOT EXISTS idx_rag_embedding_ivfflat
    ON rag_document_chunk
    USING ivfflat (embedding vector_cosine_ops)
    WITH (lists = 100);
```

**注意**：

- `ivfflat` 在数据很少时效果一般，面试可说明：**先暴力检索/小表线性扫描，数据增长后再上 ANN 索引**。
- 跨 **MySQL 与 PostgreSQL** 时，业务上常用 `memory_uuid` 或 `session_id` 关联，接受**最终一致**需在架构说明中写清。

---

## 快速面试话术（30 秒）

「编排层用 LangGraph4j 做 P&E，Planner 出任务列表，子任务内 ReAct，并用 `maxSteps` 和 CheckResult 兜底；工具失败、检索空、专家冲突走 RePlan 写事件表；Reflect 产出 JSON 归因进策略库。记忆分 Working 窗口+importance、Experiential 摘要+pgvector+decay、Semantic 用 NLI 控冲突，记忆通过工具按需拉。治理上用 Advisor 打 Trace 落 MySQL，带租户与节点名便于审计。工具集中注册，MCP 侧 TLS、超时、熔断、按租户限流。」

---

*文档版本：与仓库同步维护；DDL 为设计参考，上线前需经 DBA 评审与迁移策略确认。*
