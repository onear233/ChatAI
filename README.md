# ChatAI — AI Agent Platform / AI 智能体平台

A Spring Boot 4.0 + Java 21 AI agent platform with multi-model LLM support, plugin-based Skill marketplace, MCP protocol compatibility, knowledge base & role simulation, and long-term persistent memory.

基于 Spring Boot 4.0 + Java 21 的 AI 智能体平台，支持多模型 LLM 配置、插件式 Skill 市场、MCP 协议兼容、知识库与角色模拟，以及长久化持久记忆。

## Architecture / 系统架构

```
┌─────────────────────────────────────────────────────────────────┐
│                     Web Console (MD3 SPA)                        │
│              /console.html  ·  REST API consumers                │
└──────────────────────────┬──────────────────────────────────────┘
                           │ HTTP
┌──────────────────────────┴──────────────────────────────────────┐
│                    Web Layer (controllers/)                       │
│  AgentController │ ModelController │ SkillController             │
│  McpController   │ KnowledgeController │ MemoryController        │
└──────────────────────────┬──────────────────────────────────────┘
                           │
              ┌────────────┼────────────┐
              ▼            ▼            ▼
┌─────────────────┐ ┌───────────┐ ┌──────────────┐
│  Agent Layer     │ │  Model    │ │  Memory      │
│  Orchestrator   │ │  Layer    │ │  Layer       │
│  Session mgmt   │ │           │ │              │
│  Tool-call loop │ │ Registry  │ │ Short-term   │
│  Memory extract │ │ Provider  │ │ Long-term    │
│  KB injection   │ │ Interface │ │ File persist │
└────────┬────────┘ └─────┬─────┘ └──────┬───────┘
         │                │              │
         └────────────────┼──────────────┘
                          │
┌─────────────────────────┴───────────────────────────────────────┐
│                    Skill Marketplace                             │
│  Skill interface · Registry · Loader · ClassLoader · Manager    │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐                      │
│  │ Weather  │  │Calculator│  │WebSearch │  ← built-in           │
│  └──────────┘  └──────────┘  └──────────┘                      │
│  ┌──────────────────────────────────────────┐                   │
│  │  External JARs → skills/  (hot-loaded)   │ ← user-uploaded   │
│  └──────────────────────────────────────────┘                   │
└─────────────────────────────────────────────────────────────────┘
                          │
┌─────────────────────────┴───────────────────────────────────────┐
│                      MCP Layer                                   │
│  ProtocolHandler · Client · FileSystemMcpServer                 │
│  JSON-RPC 2.0 tools/list · tools/call                           │
└─────────────────────────────────────────────────────────────────┘
```

## Package Structure / 包结构

```
com.onear.chatai
├── ChatAiApplication.java        # Spring Boot entry point / 启动入口
├── agent/                        # Agent orchestration / 智能体编排
│   ├── AgentOrchestrator.java    # Main loop: prompt → call → tool-use → memory
│   ├── AgentContext.java         # Session-scoped state / 会话级上下文
│   └── AgentSession.java         # Conversation session / 对话会话
├── model/                        # LLM abstraction / 大模型抽象层
│   ├── ModelProvider.java        # Interface: chat(systemPrompt, history, options)
│   ├── ModelInfo.java            # Record: id, name, type, endpoint, key, modelName
│   ├── ModelRegistry.java        # Runtime model registration & switching / 运行时模型注册与切换
│   ├── ModelProperties.java      # @ConfigurationProperties for YAML model configs
│   ├── ApiKeyStore.java          # In-memory API key storage (never persisted) / 内存密钥存储
│   └── impl/
│       ├── AbstractHttpModelProvider.java  # Shared HTTP + JSON logic
│       ├── OpenAiProvider.java             # OpenAI /v1/chat/completions
│       ├── QwenProvider.java               # DashScope compatible-mode / 通义千问
│       ├── DeepSeekProvider.java           # DeepSeek V4 (thinking/reasoning) / 深度求索
│       └── LocalModelProvider.java         # Ollama / vLLM local endpoints
├── memory/                       # Conversation memory / 对话记忆
│   ├── ChatMessage.java          # Record: role, content, timestamp
│   ├── ChatMemory.java           # Interface: add, getHistory, clear
│   ├── MemoryManager.java        # Coordinates short-term + file backends
│   ├── LongTermMemory.java       # Persistent memory (JSON file, auto-extraction)
│   └── impl/
│       ├── InMemoryChatMemory.java  # ConcurrentHashMap sliding window
│       └── FileBasedMemory.java     # JSON file persistence per session
├── skill/                        # Plugin Skill marketplace / 插件技能市场
│   ├── Skill.java                # Interface: getName, getMetadata, execute
│   ├── SkillMetadata.java        # Record: name, desc, version, schema, deps
│   ├── SkillResult.java          # Record: success, content, data
│   ├── SkillParam.java           # @interface annotation for parameter metadata
│   ├── SkillRegistry.java        # Thread-safe skill map with enable/disable
│   ├── SkillManager.java         # Boot loader + WatchService hot-load / 启动加载 + 热加载
│   ├── SkillLoader.java          # JAR scanning (SPI + classpath) / JAR 扫描
│   ├── SkillClassLoader.java     # Parent-last URLClassLoader for isolation / 类加载隔离
│   └── builtin/
│       ├── WeatherSkill.java     # Mock weather by city name / 模拟天气查询
│       ├── CalculatorSkill.java  # Safe math parser (+, -, *, /, ^, parens) / 安全数学解析器
│       └── WebSearchSkill.java   # Mock web search results / 模拟网页搜索
├── knowledge/                    # Knowledge base & role simulation / 知识库与角色模拟
│   ├── KnowledgeEntry.java       # Record: id, name, type, content, active
│   └── KnowledgeManager.java     # File-persisted KB manager / 文件持久化管理
├── mcp/                          # MCP protocol / MCP 协议
│   ├── McpServer.java            # Interface: getTools, callTool
│   ├── McpProtocolHandler.java   # JSON-RPC 2.0 message handler
│   ├── McpClient.java            # Connect to external MCP servers / 连接外部 MCP 服务
│   ├── McpToolDefinition.java    # Record: name, description, inputSchema
│   └── example/
│       └── FileSystemMcpServer.java  # Sandboxed file read/write/list / 沙箱文件操作
├── web/
│   ├── controller/
│   │   ├── AgentController.java      # POST /api/chat, session CRUD
│   │   ├── ModelController.java      # Model list, switch, API key, enable/disable
│   │   ├── SkillController.java      # Skill list, upload JAR, enable/disable, delete
│   │   ├── McpController.java        # MCP server/tool listing, connect, call
│   │   ├── KnowledgeController.java  # Knowledge CRUD, TXT upload, activate
│   │   └── MemoryController.java     # Long-term memory CRUD / 长久记忆管理
│   └── dto/
│       ├── ChatRequest.java
│       ├── ChatResponse.java
│       └── SkillUploadRequest.java
└── config/
    ├── McpConfig.java            # Model provider factory + mock fallback
    └── WebMvcConfig.java         # CORS configuration / 跨域配置
```

## Quick Start / 快速开始

### Prerequisites / 环境要求

- JDK 21 (`D:/openjdk21/jdk-21.0.2` or equivalent / 或等效版本)
- Maven wrapper included / 内置 Maven wrapper (`./mvnw`)

### Build & Run / 构建与运行

```bash
# Build / 构建
./mvnw package -DskipTests -Dmaven.compiler.executable="<path-to-jdk21>/bin/javac"

# Run (default port 8080) / 运行（默认端口 8080）
java -jar target/ChatAI-0.0.1-SNAPSHOT.jar

# Run on custom port / 指定端口运行
java -jar target/ChatAI-0.0.1-SNAPSHOT.jar --server.port=8081
```

Open `http://localhost:8080/console.html` for the web console. / 打开上述地址访问 Web 控制台。

### Default Models / 默认模型

The platform ships with 4 **mock models** for development (no API key needed). Real model providers (OpenAI, Qwen, DeepSeek, Local) are pre-configured but disabled — set an API key in the console to enable them.

平台内置 4 个 **mock 模型**用于开发调试（无需 API Key）。真实模型提供商（OpenAI、Qwen、DeepSeek、Local）已预配置但默认禁用——在控制台中设置 API Key 即可启用。

## Model Configuration / 模型配置

Models are defined in `application.yml` under `agent.models`:

模型在 `application.yml` 的 `agent.models` 下定义：

```yaml
agent:
  active-model: mock-general
  models:
    - id: deepseek-v4-pro
      name: DeepSeek V4 Pro
      type: DEEPSEEK
      api-endpoint: https://api.deepseek.com/chat/completions
      api-key: ${DEEPSEEK_API_KEY:}      # env var with fallback / 环境变量，可回退为空
      model-name: deepseek-v4-pro
      enabled: false
      config:
        description: "Flagship V4 with thinking mode"  # 旗舰 V4，支持思考模式
        icon: psychology
        color: "#3f51b5"
        thinking: true                     # DeepSeek V4 thinking mode / 深度思考模式
        reasoning_effort: "high"           # V4 reasoning effort level / 推理强度
```

**Supported provider types / 支持的提供商类型**: `MOCK`, `OPENAI`, `QWEN`, `DEEPSEEK`, `LOCAL`

## API Reference / API 参考

### Chat / 对话

| Method | Endpoint | Description / 说明 |
|--------|----------|-------------|
| `POST` | `/api/chat` | Send message, get AI response / 发送消息，获取 AI 回复 |
| `GET`  | `/api/sessions/{id}/history` | Get conversation history / 获取对话历史 |
| `POST` | `/api/sessions/{id}/reset` | End conversation, keep persistent memory / 结束对话，保留持久记忆 |
| `DELETE` | `/api/sessions/{id}` | Delete session / 删除会话 |

### Models / 模型

| Method | Endpoint | Description / 说明 |
|--------|----------|-------------|
| `GET` | `/api/models` | List all models with status / 列出所有模型及状态 |
| `PUT` | `/api/models/active/{id}` | Switch active model / 切换当前模型 |
| `PUT` | `/api/models/{id}/key` | Set API key (runtime only) / 设置 API Key（仅运行时） |
| `PUT` | `/api/models/{id}/enable` | Enable model / 启用模型 |
| `PUT` | `/api/models/{id}/disable` | Disable model / 禁用模型 |

### Skills / 技能

| Method | Endpoint | Description / 说明 |
|--------|----------|-------------|
| `GET` | `/api/skills` | List all skills with metadata / 列出所有技能及元数据 |
| `POST` | `/api/skills/upload` | Upload skill JAR / 上传技能 JAR |
| `PUT` | `/api/skills/{name}/enable` | Enable skill / 启用技能 |
| `PUT` | `/api/skills/{name}/disable` | Disable skill / 禁用技能 |
| `DELETE` | `/api/skills/{name}` | Remove skill + delete JAR / 移除技能并删除 JAR |

### Knowledge Base / 知识库

| Method | Endpoint | Description / 说明 |
|--------|----------|-------------|
| `GET` | `/api/knowledge` | List knowledge entries / 列出知识条目 |
| `POST` | `/api/knowledge/text` | Add knowledge from text input / 通过文本添加知识 |
| `POST` | `/api/knowledge/upload` | Upload TXT file / 上传 TXT 文件 |
| `PUT` | `/api/knowledge/{id}/active` | Activate knowledge base / 激活知识库 |
| `DELETE` | `/api/knowledge/{id}` | Delete knowledge entry / 删除知识条目 |

### Long-term Memory / 长久记忆

| Method | Endpoint | Description / 说明 |
|--------|----------|-------------|
| `GET` | `/api/memory/longterm` | List persistent memories / 列出持久记忆 |
| `POST` | `/api/memory/longterm` | Add memory manually / 手动添加记忆 |
| `DELETE` | `/api/memory/longterm/{id}` | Delete memory entry / 删除记忆条目 |
| `DELETE` | `/api/memory/longterm` | Clear all memories / 清空所有记忆 |

### MCP

| Method | Endpoint | Description / 说明 |
|--------|----------|-------------|
| `GET` | `/api/mcp/servers` | List MCP servers / 列出 MCP 服务 |
| `POST` | `/api/mcp/servers` | Connect external MCP server / 连接外部 MCP 服务 |
| `GET` | `/api/mcp/tools` | List all MCP tools / 列出所有 MCP 工具 |
| `POST` | `/api/mcp/tools/{name}/call` | Call MCP tool / 调用 MCP 工具 |

---

# Skill Development Guide / Skill 开发指南

## Overview / 概述

Skills are the plugin system of ChatAI. Each Skill is a self-contained Java class that:

Skill 是 ChatAI 的插件系统。每个 Skill 是一个独立的 Java 类，需满足：

1. Declares **metadata** (name, description, version, parameter JSON Schema, dependencies) — 声明**元数据**（名称、描述、版本、参数 JSON Schema、依赖）
2. Implements an **execute** method that takes parameters and returns results — 实现 **execute** 方法，接收参数并返回结果
3. Is discovered automatically via SPI or classpath scanning when packaged in a JAR — 打包为 JAR 后通过 SPI 或类路径扫描自动发现

Skills participate in the agent's tool-calling loop: the AI receives the skill schemas in its system prompt and invokes them via `[TOOL:skillName {"param":"value"}]` syntax. The orchestrator parses these tool calls, executes the skill, and injects the result back.

Skill 参与智能体的工具调用循环：AI 在系统提示中获取 skill 的 schema，通过 `[TOOL:skillName {"param":"value"}]` 语法调用。编排器解析工具调用、执行 skill、并将结果注入回对话。

## Skill Interface / Skill 接口

```java
package com.onear.chatai.skill;

import java.util.Map;

public interface Skill {

    /** Unique skill name used for tool invocation / 工具调用的唯一名称: [TOOL:weather ...] */
    String getName();

    /** Metadata for the marketplace and AI tool schema generation / 市场和 AI 工具 schema 生成的元数据 */
    SkillMetadata getMetadata();

    /** Execute the skill with named parameters / 使用命名参数执行技能 */
    SkillResult execute(Map<String, Object> params);

    /** Override to disable by default / 重写以默认禁用 */
    default boolean isEnabled() { return true; }
}
```

## SkillMetadata Record / SkillMetadata 记录

```java
public record SkillMetadata(
    String name,                          // Unique identifier / 唯一标识
    String description,                   // Human-readable, shown in marketplace / 可读描述，在市场页展示
    String version,                       // Semantic version e.g. "1.0.0" / 语义版本号
    Map<String, Object> parameterSchema,  // JSON Schema for parameters / 参数 JSON Schema
    List<String> dependencies,            // External library coordinates (informational) / 外部依赖（信息性）
    String author                         // Author name / 作者
) {}
```

## SkillResult Record / SkillResult 记录

```java
public record SkillResult(
    boolean success,               // true = normal, false = error / true = 正常, false = 错误
    String content,                // Display text returned to the AI / 返回给 AI 的展示文本
    Map<String, Object> data       // Optional structured data / 可选的结构化数据
) {
    static SkillResult ok(String content);
    static SkillResult ok(String content, Map<String, Object> data);
    static SkillResult error(String content);
}
```

## Step-by-Step: Creating a New Skill / 分步教程：创建新 Skill

### Step 1 / 第一步: Create a Maven project / 创建 Maven 项目

```xml
<!-- pom.xml -->
<project>
    <modelVersion>4.0.0</modelVersion>
    <groupId>com.example</groupId>
    <artifactId>my-skill</artifactId>
    <version>1.0.0</version>

    <dependencies>
        <dependency>
            <groupId>com.onear</groupId>
            <artifactId>ChatAI</artifactId>
            <version>0.0.1-SNAPSHOT</version>
            <scope>provided</scope>   <!-- platform provides it / 平台提供 -->
        </dependency>
    </dependencies>
</project>
```

### Step 2 / 第二步: Implement the Skill interface / 实现 Skill 接口

```java
package com.example.skill;

import com.onear.chatai.skill.*;
import java.util.*;

public class TranslateSkill implements Skill {

    @Override
    public String getName() { return "translate"; }

    @Override
    public SkillMetadata getMetadata() {
        return new SkillMetadata(
            "translate",
            "Translate text between languages",               // 语言间文本翻译
            "1.0.0",
            Map.of(
                "type", "object",
                "properties", Map.of(
                    "text", Map.of("type", "string",
                                   "description", "Text to translate"),       // 待翻译文本
                    "from", Map.of("type", "string",
                                   "description", "Source language code (e.g. en)"),  // 源语言代码
                    "to", Map.of("type", "string",
                                 "description", "Target language code (e.g. zh)")     // 目标语言代码
                ),
                "required", List.of("text", "to")
            ),
            List.of(),     // no dependencies / 无依赖
            "Your Name"
        );
    }

    @Override
    public SkillResult execute(Map<String, Object> params) {
        String text = (String) params.get("text");
        String to = (String) params.get("to");

        // Your translation logic here / 在此处编写翻译逻辑
        String translated = callTranslationAPI(text, to);

        return SkillResult.ok(translated, Map.of(
            "original", text,
            "translated", translated,
            "targetLang", to
        ));
    }

    private String callTranslationAPI(String text, String to) {
        // Implementation / 具体实现
        return "[translated] " + text;
    }
}
```

### Step 3 / 第三步: Register via SPI / 通过 SPI 注册

Create / 创建 `META-INF/services/com.onear.chatai.skill.Skill`:

```
com.example.skill.TranslateSkill
```

### Step 4 / 第四步: Package and upload / 打包并上传

```bash
mvn package
# Upload my-skill-1.0.0.jar via the web console at /console.html → Skills tab
# 通过 Web 控制台 /console.html → 技能 页面上传 my-skill-1.0.0.jar
```

Or place the JAR directly in the `skills/` directory — it will be hot-loaded within 5 seconds.

或者将 JAR 直接放入 `skills/` 目录——将在 5 秒内自动热加载。

## How Skill Discovery Works / Skill 发现机制

```
skills/your-skill.jar
  │
  ▼
SkillClassLoader (parent-last, isolated)   # 父类后置、类加载隔离
  │
  ├── 1. SPI: META-INF/services/com.onear.chatai.skill.Skill
  │      → reads fully-qualified class names / 读取全限定类名
  │
  └── 2. Classpath scan: enumerates .class entries / 类路径扫描：枚举 .class 条目
         → checks if Skill.class.isAssignableFrom(cls) / 检查是否实现 Skill 接口
  │
  ▼
SkillRegistry.register(skill, builtin=false, sourceJar="your-skill.jar")
  │
  ▼
Agent system prompt includes skill schema   # Agent 系统提示包含 skill schema
  │
  ▼
AI outputs: [TOOL:translate {"text":"hello","to":"zh"}]  # AI 输出工具调用
  │
  ▼
Orchestrator parses → Skill.execute(params) → result injected  # 编排器解析 → 执行 → 注入结果
```

## ClassLoader Isolation / 类加载器隔离

`SkillClassLoader` extends `URLClassLoader` with **parent-last** delegation:

`SkillClassLoader` 继承 `URLClassLoader`，采用**父类后置**委托策略：

- **Skill classes** loaded from the JAR first — Skill 类优先从 JAR 加载
- **Platform classes** (`java.*`, `jakarta.*`, `org.springframework.*`, `tools.jackson.*`, `org.slf4j.*`, `com.onear.chatai.skill.*`) delegated to parent — 平台类委托给父加载器
- Prevents skill JARs from polluting the main classpath or conflicting with framework versions — 防止 Skill JAR 污染主类路径或与框架版本冲突

## Built-in Skills Reference / 内置 Skill 参考

### WeatherSkill / 天气查询
```json
{
  "name": "weather",
  "description": "Get current weather for a city / 获取城市当前天气",
  "parameters": {
    "city": { "type": "string", "description": "City name / 城市名称" }
  }
}
```

### CalculatorSkill / 数学计算
```json
{
  "name": "calculator",
  "description": "Evaluate a mathematical expression. Supports +, -, *, /, parentheses, and exponentiation. / 计算数学表达式，支持 +、-、*、/、括号和幂运算。",
  "parameters": {
    "expression": { "type": "string", "description": "Math expression to evaluate / 待计算的数学表达式" }
  }
}
```

### WebSearchSkill / 网页搜索
```json
{
  "name": "websearch",
  "description": "Search the web for information. Returns formatted search result snippets. / 搜索网络信息，返回格式化的搜索结果摘要。",
  "parameters": {
    "query": { "type": "string", "description": "Search query / 搜索关键词" }
  }
}
```

## Tool-Calling Protocol / 工具调用协议

The orchestrator injects available skill schemas into the system prompt. The AI invokes tools with this syntax:

编排器将可用 skill 的 schema 注入系统提示。AI 使用以下语法调用工具：

```
[TOOL:skillName {"param1":"value1","param2":"value2"}]
```

**Multi-turn tool use / 多轮工具调用**: If the AI's response contains `[TOOL:...]`, the orchestrator executes the tool, injects the result, and calls the model again (up to 3 iterations). This allows the AI to chain multiple tool calls.

如果 AI 回复中包含 `[TOOL:...]`，编排器执行该工具，注入结果，并再次调用模型（最多 3 轮迭代），从而支持链式多工具调用。

**Memory extraction / 记忆提取**: In parallel, the AI can record persistent facts:

与此同时，AI 可以记录持久化事实：

```
[MEMORY:user_name] John
```

The orchestrator extracts these, stores them in `LongTermMemory`, and strips the tag from the visible response.

编排器提取这些内容，存入 `LongTermMemory`，并从可见回复中移除标签。

## Knowledge Base & Role System / 知识库与角色系统

Knowledge and role definitions are injected into the system prompt before every chat call:

知识库和角色定义在每次对话调用前注入系统提示：

- **Knowledge Base** (`type=knowledge`): Injects reference facts the AI should use — 注入 AI 应参考的事实信息
- **Role Simulation** (`type=role`): Injects a persona definition; AI stays in character — 注入角色定义，AI 保持角色扮演

Upload via the web console (Knowledge tab) or API:

通过 Web 控制台（知识库标签页）或 API 上传：

```bash
# Upload TXT file / 上传 TXT 文件
curl -X POST http://localhost:8080/api/knowledge/upload \
  -F "file=@knowledge.txt" -F "type=knowledge"

# Or paste text directly / 或直接粘贴文本
curl -X POST http://localhost:8080/api/knowledge/text \
  -H "Content-Type: application/json" \
  -d '{"name":"Pirate","type":"role","content":"You are a pirate. Talk like a pirate. / 你是一个海盗，用海盗的口吻说话。"}'
```

## Long-term Memory / 长久记忆

Memories persist across sessions as JSON files under `data/memory/longterm.json`. The AI auto-extracts memories during conversation; they can also be managed manually via the Memory API or the Knowledge tab in the web console.

记忆以 JSON 文件形式持久化在 `data/memory/longterm.json`，跨会话保留。AI 在对话中自动提取记忆；也可通过 Memory API 或 Web 控制台的知识库标签页手动管理。

---

## Tech Stack / 技术栈

| Layer / 层级 | Technology / 技术 |
|-------|-----------|
| Framework / 框架 | Spring Boot 4.0.6 |
| Language / 语言 | Java 21 |
| JSON | Jackson 3.x (`tools.jackson`) |
| Frontend / 前端 | Vanilla JS + Material Design 3 CSS |
| Persistence / 持久化 | JSON files under `data/` / `data/` 下的 JSON 文件 |
| Build / 构建 | Maven |
