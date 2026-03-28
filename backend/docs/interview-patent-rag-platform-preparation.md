# 基于 RAG 的专利成果转化平台 — 面试准备手册

本文档围绕简历条目 **「2025.02–2025.07｜基于 RAG 的专利成果转化平台｜核心成员」**，按**架构与鉴权、RAG 管线、混合检索与评估、CRAG 门控、可量化指标、安全与一致性**组织，便于答辩时**说清技术逻辑、数据来源与局限**。  
与本仓库实现可对读：`backend/docs/TECHNICAL-DOCUMENTATION.md`、`AI-TECHNICAL-DOCUMENTATION-FULL.md`、`ENTERPRISE_FEATURES_IMPLEMENTED.md`、`scripts/README-ragas.md`。

**说明**：文中「MQE」按检索领域常见含义写作 **Multi-Query Expansion（多查询扩展）**；若你当时指其他缩写（如某内部项目名），面试时口头对齐即可。

---

## 目录

1. [项目背景与简历定位](#1-项目背景与简历定位)
2. [业务中台与 JWT 鉴权](#2-业务中台与-jwt-鉴权)
3. [RAG 文档处理与查询增强](#3-rag-文档处理与查询增强)
4. [Hybrid 检索、融合排序与评测闭环](#4-hybrid-检索融合排序与评测闭环)
5. [CRAG 式检索后门控](#5-crag-式检索后门控)
6. [可量化指标体系（定义、计算方式、数据来源）](#6-可量化指标体系定义计算方式数据来源)
7. [库表与 Redis 设计（面试级）](#7-库表与-redis-设计面试级)
8. [性能、成本与 SLO 口径](#8-性能成本与-slo-口径)
9. [与仓库文档的对照](#9-与仓库文档的对照)
10. [30 秒面试话术](#10-30-秒面试话术)

---

## 1. 项目背景与简历定位

| 维度 | 答辩要点 |
|------|----------|
| **问题** | 专利技术理解成本高、转化决策难；需要「可检索、可对比、可解释」的专利转化情报与问答能力。 |
| **价值** | 面向企业/高校/个人：专利转化智能查询、成果调研；为领域研究提供结构化专利转化数据支撑。 |
| **你的角色** | 核心成员：强调**中台与鉴权**、**RAG 全链路（切分→检索→重排→门控→评估）**中的可落地设计与指标。 |
| **技术栈** | Spring Boot、MySQL、Redis、JWT、RAG、Hybrid（向量+BM25）、RRF、Rerank（如 Cohere）、CRAG、RAGAS 等。 |

---

## 2. 业务中台与 JWT 鉴权

### 2.1 业务中台（行业常见做法）

- **统一领域**：用户、专利检索会话、收藏、反馈、评测任务等 RESTful 资源；**版本化 API**（`/api/v1/`）。
- **边界**：中台负责 **鉴权、租户/用户上下文、限流、审计日志**；检索与向量服务可内嵌或独立部署，通过客户端或网关调用。

### 2.2 JWT 鉴权与 Token 刷新（实现逻辑）

| 概念 | 说明 |
|------|------|
| **Access Token** | 短有效期（如 15–60 分钟），放 `Authorization: Bearer`，用于 API 鉴权。 |
| **Refresh Token** | 长有效期（如 7–30 天），**仅用于**换新 Access Token；**禁止**频繁携带业务请求。 |
| **失效** | 登出、改密、风控：将 `refresh_token_id`（或 `jti`）加入 **Redis 黑名单**或 DB **revoked 表**，校验时拒绝。 |
| **自动续期** | 前端在 Access 将过期前用 Refresh 换一对新 Token（滑动会话）；服务端校验 Refresh 未吊销且未过期。 |

**面试一句话**：「双 Token + Redis 黑名单/版本号 + Refresh 旋转（rotation），平衡安全与体验。」

### 2.3 安全与分布式注意点

- **HTTPS** 全站；Refresh **HttpOnly + Secure Cookie** 或安全存储（移动端）。
- **同设备并发刷新**：Refresh 旋转时用 **一次性** 新 Refresh，旧 Refresh 立即失效，防重放。
- **Redis**：黑名单/会话 TTL 与 Access TTL 对齐；**Key** 建议 `auth:revoke:{jti}` 或 `user:{id}:refresh_version`。

---

## 3. RAG 文档处理与查询增强

### 3.1 文档切分：递归 vs 语义 + Token 回退

| 策略 | 特点 | 典型问题 |
|------|------|----------|
| **递归切分** | 按固定字符/分隔符递归切，实现简单 | 长段落可能在**句子中间**切断，语义断裂。 |
| **语义切分 + Token 回退** | 先按语义边界（段落/标题/语义相似度断点）切，再按 **token 上限**合并或二次切，避免超模型上下文 | 工程复杂度更高，需 tokenizer 对齐 |

**简历表述**：对比传统递归切分与「语义切分 + Token 回退」，选用后者以**提高长文信息保留率**、缓解长文本**语义断裂**。

### 3.2 查询增强：MQE（多查询）与 HyDE

| 机制 | 作用 | 面试要点 |
|------|------|----------|
| **Multi-Query Expansion（MQE）** | 用 LLM 或规则将用户 **1 个 query 扩展为多个子查询**，多路检索后 **RRF 融合** | 提升**召回**（同义表达、不同角度） |
| **HyDE（Hypothetical Document Embeddings）** | 用 LLM 生成**假设性答案段落**，对**假设段落**做向量嵌入再检索 | 拉近 query 与文档在向量空间的分布，常**提升向量路召回**；BM25 仍可用**原始 query** 保持关键词命中 |

**检索链顺序（常见）**：多查询（可选）→ 向量路（可选 HyDE）+ BM25 → **RRF** → **Rerank** → CRAG 门控（若启用）→ 生成。

---

## 4. Hybrid 检索、融合排序与评测闭环

### 4.1 混合召回与融合

| 环节 | 常见实现 | 说明 |
|------|----------|------|
| **向量检索** | 相似度 TopK + **相似度阈值**过滤低分噪声 | 控制「勉强相关」的 chunk 进入生成 |
| **BM25** | 关键词/专利号/术语精确匹配 | 与向量互补 |
| **RRF（Reciprocal Rank Fusion）** | 多路排序列表融合为单一排序 | 降低单路排序偏差，**不依赖**分数归一化到同一量纲 |
| **Rerank（如 Cohere）** | 对 RRF 后的候选用 **query–doc 交叉编码**精排 | 在 **finalTopK** 内提升 Precision，控制 token |

### 4.2 RAGAS 与自动化评估 + 人工审核

- **RAGAS**：对 **Faithfulness、Answer Relevance、Context Recall** 等指标用 LLM 或启发式打分（具体指标集以项目配置为准）。
- **闭环**：Java 侧导出「检索上下文 + 生成答案」→ Python `ragas_eval.py` 批跑 → 回归对比版本。
- **高质量评测集**：**LLM 批量生成候选 QA** + **人工审核**修正标签与引用，减少噪声标签导致的「虚高/虚低」。

**简历数字（答辩时）**：Recall@20 从 **62% → 80%**；**Top@1 准确率提升约 10 个百分点**——需能说明：**评测集范围、固定随机种子、同一模型/同一检索参数**，避免被追问「是否同一批数据 cherry-pick」。

---

## 5. CRAG 式检索后门控

### 5.1 概念（对齐论文 CRAG / 工程化）

在**底层检索链完成之后**、进入大模型生成之前，对检索到的文档（或上下文）做**置信度/相关性评估**，再按状态路由后续动作，减少**无效生成**与**错误决策**。

### 5.2 三态路由（CORRECT / AMBIGUOUS / INCORRECT）

| 状态 | 含义（工程上可定义） | 常见后续动作 |
|------|----------------------|--------------|
| **CORRECT** | 检索结果与问题高度相关，可支撑回答 | 直接进入生成；或轻度压缩上下文 |
| **AMBIGUOUS** | 部分相关、证据不足或冲突 | **过滤/重排 chunk**、**查询改写**再检、或缩小生成范围并提示不确定性 |
| **INCORRECT** | 检索不相关或置信度过低 | **不采用该上下文**、触发 **Web 搜索兜底** / 拒答模板 / 要求用户补充信息 |

**评分来源**：轻量交叉编码器、LLM-as-judge、或规则（最高分、Top1–Top2 分差、空结果）。需与**空上下文策略**统一，避免两套逻辑打架。

### 5.3 与「检索前」增强的区别

- **HyDE/MQE/RRF**：解决「**找得全、排得准**」。
- **CRAG 门控**：解决「**找来的东西该不该用**」——**检索后**质量把关。

---

## 6. 可量化指标体系（定义、计算方式、数据来源）

**原则**：指标必须明确 **分母**（每次请求 / 每个 query / 每条评测样本）与 **环境**（离线评测集 vs 线上日志）。

### 6.1 检索与排序质量（离线为主）

| 指标 | 定义 / 公式 | 怎样得到 | 数据来源 |
|------|----------------|----------|----------|
| **Recall@K** | 相关文档是否出现在 TopK 中 | 评测集标注「相关 doc_id」集合；若命中至少一个即为 1，否则 0；对样本平均 | **标注数据集** + 检索日志输出 `ranked_doc_ids` |
| **Recall@20（简历）** | K=20 的 Recall | 同上 | RAGAS 自定义或自建脚本；**固定评测集**版本号 |
| **Top@1 准确率 / MRR** | Top1 是否相关，或平均倒数排名 | 需**单 gold 或主 relevant** 定义清晰 | 同上 |
| **nDCG@K** | 考虑排序位置的加权增益 | 多相关度等级时更合适 | 评测集 + 排序列表 |
| **Context Precision（RAGAS）** | 检索上下文中有用比例 | RAGAS 内置 | `scripts/ragas_eval.py` 导出结果 |

### 6.2 生成质量（离线）

| 指标 | 说明 | 来源 |
|------|------|------|
| **Faithfulness** | 答案是否可由上下文支撑 | RAGAS / LLM judge |
| **Answer Relevance** | 答案是否切题 | RAGAS |
| **人工审核通过率** | 抽检样本中「可发布」比例 | 评测工单、标注平台 |

### 6.3 线上与工程指标

| 指标 | 定义 | 数据来源 |
|------|------|----------|
| **检索延迟 P95** | 从收到 query 到 Rerank 结束 | APM、检索服务 metrics |
| **门控分布** | CORRECT/AMBIGUOUS/INCORRECT 占比 | CRAG 模块打点 + 日志 |
| **空上下文率** | 无可用 chunk 的请求占比 | 检索结果计数 + 网关日志 |
| **JWT 校验失败率** | 401 次数 / 请求次数 | 网关、应用 access log |
| **Token 成本** | 每请求/每用户 LLM token | 提供商账单、应用 `metadata` |

### 6.4 简历数字如何被质疑时的答法

- 「Recall@20 从 62% 到 80%」：**同一评测集**、**同一标注规范**、**优化前后各跑一遍**；改进点可对应 **切分 + MQE + HyDE + RRF + Rerank + 阈值** 中的子集组合。
- 「Top@1 提升 10%」：明确是 **准确率** 还是 **MRR**；分母是**仅有关键词 query** 还是**全量**。

---

## 7. 库表与 Redis 设计（面试级）

### 7.1 MySQL（示例字段）

**用户与刷新令牌（若 Refresh 存库）**

```sql
-- 示例：refresh_token 存储（rotation）
CREATE TABLE user_refresh_token (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  token_id CHAR(36) NOT NULL COMMENT 'jti',
  expires_at TIMESTAMP NOT NULL,
  revoked TINYINT(1) NOT NULL DEFAULT 0,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_token_id (token_id),
  KEY idx_user (user_id)
);
```

**RAG 侧（可选）**：`document`、`chunk`（`doc_id`, `chunk_index`, `content_hash`, `token_count`）、`ingestion_job` 等。

### 7.2 Redis

- `auth:blacklist:{jti}` → TTL 与剩余 Access 寿命一致。  
- `ratelimit:{user_id}:{api}` → 滑动窗口限流。  
- 热点检索结果缓存（慎用）：**Key 含 query hash + 版本号**，避免旧索引。

---

## 8. 性能、成本与 SLO 口径

| 维度 | 经验量级（答辩用「数量级」，以你们实测为准） |
|------|----------------------------------------------|
| **端到端问答 P95** | 简单 RAG **3–15s**；含多查询+HyDE+Rerank **10–40s** |
| **检索子链路 P95** | **200ms–2s**（视向量库规模与是否远程 Rerank） |
| **错误率** | 依赖服务超时目标 **小于 1%**（重试后） |
| **成本** | HyDE/MQE/重排均增加 **LLM 调用与 Rerank API**；可用 **门控**与 **缓存**压调用量 |

---

## 9. 与仓库文档的对照

| 文档 | 与本项目叙述的对应关系 |
|------|------------------------|
| `TECHNICAL-DOCUMENTATION.md` | Spring AI RAG 模块、RRF、Rerank、RAGAS 脚本入口 |
| `AI-TECHNICAL-DOCUMENTATION-FULL.md` | Hybrid、RRF、Embedding Rerank、专利场景为何向量+BM25 |
| `ENTERPRISE_FEATURES_IMPLEMENTED.md` | CRAG 三态、HyDE、Hybrid 内 HyDE+BM25→RRF→Rerank、检索链顺序 |
| `ENTERPRISE_AGENT_RAG_GAP_ANALYSIS.md` | CRAG/HyDE/企业 RAG 差距分析（可用于「还做过哪些演进」） |
| `scripts/README-ragas.md` | Java 导出 → Python RAGAS 评估流程 |
| `agent-graph-execution-flow.md` | 专利场景下图编排与检索专家（若面试问 Agent+RAG 结合） |

---

## 10. 30 秒面试话术

「我在专利成果转化平台里负责中台和鉴权：**Spring Boot REST + JWT 双 Token、Refresh 旋转、Redis 做吊销/黑名单**。RAG 侧对比了递归切分和**语义切分加 Token 回退**，用后者减少长文语义断裂；查询上用了**多查询扩展**和 **HyDE** 做召回增强。检索是**向量加阈值、BM25 混合**，**RRF 融合** 再用 **Cohere Rerank** 精排；离线用 **RAGAS** 做回归，**LLM 生成加人工审核**建评测集，Recall@20 从 62% 提到 80%，Top@1 也有约十个点提升。最后在检索链后加了 **CRAG 式门控**，用 **CORRECT/AMBIGUOUS/INCORRECT** 分状态路由，降低无效生成和错误决策风险。」

---

*文档用于面试准备；具体数字与模块名请以你当时代码/报告为准，并在答辩中准备好「评测集规模、基线版本、是否同机对比」三类追问。*
