# 01 — ChatClient 设计哲学：从 RestTemplate 到 AI

> **一句话观点：** ChatClient 不是 Spring 的新发明，而是它 20 年来一直在做的事——把「调用远端服务」这件事抽象成开发者最舒服的姿势。

---

## 一个想法的引子

2015 年我写第一个 Spring Boot 项目时，调第三方 HTTP 接口用 RestTemplate：

```java
// 2015 年：RestTemplate
RestTemplate rest = new RestTemplate();
String result = rest.getForObject("http://api.example.com/data", String.class);
```

2019 年，Spring 5 推出 WebClient：

```java
// 2019 年：WebClient（fluent 风格）
String result = WebClient.create("http://api.example.com")
    .get()
    .uri("/data")
    .retrieve()
    .bodyToMono(String.class)
    .block();
```

2026 年，我第一次写 Spring AI：

```java
// 2026 年：ChatClient
String result = ChatClient.create(chatModel)
    .prompt()
    .user("用一句话介绍自己")
    .call()
    .content();
```

看到第三段代码的时候，我想的不是「又学了一个新 API」，而是——**Spring 连 AI 都统一成这套模式了？**

这种熟悉感不是巧合。它是 Spring 团队 20 年来对「远程调用」这件事不断抽象、不断提炼的结果。ChatClient 不是横空出世的发明，而是这条进化链上的最新一环。

---

## Spring AI 的四层架构

拆开 Spring AI 的骨架，从底到顶分为四层。ChatClient 只是最上面那层：

```
┌─────────────────────────────────────────┐
│  ChatClient                             │  ← 你写的业务代码接触的层
│  (Fluent API, Builder, Advisors 编排)   │
├─────────────────────────────────────────┤
│  ChatModel                              │  ← 跨 Provider 的抽象接口
│  (OllamaChatModel / OpenAiChatModel)    │
├─────────────────────────────────────────┤
│  Model 客户端层                          │  ← 每个 Provider 各自的 SDK
│  (Ollama API / OpenAI SDK / Anthropic)  │
├─────────────────────────────────────────┤
│  HTTP 传输层                             │  ← 底层网络
│  (RestClient / WebClient)               │
└─────────────────────────────────────────┘
```

这个分层架构揭示了 Spring AI 的设计选择：

- **每一层只解决一个问题。** ChatModel 不管 HTTP 细节，ChatClient 不管 Provider 差异。
- **层与层之间通过接口隔离。** 你可以替换任何一层而不影响其他层。
- **开发者默认只接触最上层。** 你不必理解底层细节就能开始用。

这不是什么新鲜的设计——Spring 的 DataSource → Connection → Statement → ResultSet 也是类似的分层，JdbcTemplate 也是类似的外观模式。**Spring AI 没有发明新架构，它只是把 AI 集成塞进了 Spring 已经验证过的架构模式里。**

### 为什么这是好事

Java 开发者看到这个架构图的第一反应应该是——**我懂这个**。这跟 Spring MVC 的分层（Controller → Service → Repository → DataSource）是同一个结构逻辑。

但如果你来自 Python 的 LangChain 生态，你看到的会是完全不同的东西。LangChain 的架构更像一个**运行时图引擎**：你用 `|` 运算符把组件串成 DAG，在运行时动态调度。它灵活，但它的心智模型跟 Web 开发是两套。

---

## ChatClient 本身的三层抽象

回到 ChatClient 这个类本身，它在自己这一层内部又做了三层抽象：

### 第一层：Builder 模式

所有能变的都交给 Builder：

```java
ChatClient client = ChatClient.builder(chatModel)
    .defaultSystem("你是一个专业的 Java 技术顾问")
    .defaultFunctions("getWeather", "queryDatabase")
    .defaultAdvisors(new MessageChatMemoryAdvisor(chatMemory))
    .build();
```

和 WebClient 如出一辙：

```java
WebClient client = WebClient.builder()
    .baseUrl("https://api.example.com")
    .defaultHeader("Accept", "application/json")
    .defaultCookie("session", sessionId)
    .build();
```

同一套设计决策：**组装期和运行期分离。** Builder 锁配置，运行时只负责执行。可变的配置集中在 Builder 里，构造出来的 ChatClient 是不可变的安全对象。

这个模式的好处是隐性的但非常重要：因为 ChatClient 不可变，所以你可以安全地在多线程间共享同一个实例。这在 Web 应用里是常态——一个 Controller 是被所有请求线程共享的。

### 第二层：Fluent API

```java
chatClient.prompt().user(message).call().content();
```

这个链条本质上是一个**流式动词链**——每一步返回一个中间对象，直到最后的终端操作触发执行。

对应的 WebClient 模式：

```java
webClient.get().uri("/data").retrieve().bodyToMono(String.class).block();
```

多说一句：这种风格在 Python 的 AI 框架里很少见。LangChain 的调用方式是通过 `|` 运算符串联 Runnable，像 Unix 管道：

```python
chain = prompt | model | output_parser
result = chain.invoke({"topic": "AI"})
```

方式没有优劣——Python 的 pipe 模式在构建复杂 DAG 时更灵活。但关键区别在于：**Spring 开发者拿到 ChatClient 直接就能写，零学习曲线。** 这不是能力问题，这是设计哲学的差异——Spring 选择降低「第一次使用」的门槛。

### 第三层：Provider 无关

同一个 ChatClient API，底层可以是 Ollama、DeepSeek、OpenAI、Claude。切换只需改配置：

```yaml
# 本地
spring:
  ai:
    chat:
      client: ollama
      ollama:
        chat:
          options:
            model: qwen2.5:7b
```

```yaml
# 云端
spring:
  ai:
    chat:
      client: deepseek
      deepseek:
        api-key: ${DEEPSEEK_API_KEY}
```

像不像 `spring.datasource.url` 的切换？**Spring 把 AI Provider 当成了一种 DataSource 来抽象。** 数据库连接池换数据源不改业务代码，AI Provider 换模型也不改业务代码——这是同一个抽象层思想在 AI 领域的映射。

---

## Advisors 链——AI 调用的拦截器

如果说 ChatClient 是 Spring AI 的门面，那 Advisors 就是它的**灵魂架构**。

这是一个很多人没注意到但最重要的设计。

### 问题

在真实业务里，你调用 AI 不只是「发消息→收回复」。你还需要：

- 带上对话历史（多轮记忆）
- 先从向量库检索相关文档（RAG）
- 对输入做安全检查（内容审核）
- 对输出做格式校验

如果每个功能都要在 ChatClient 调用前后手动处理，代码很快就会变成一团乱麻：

```java
// 不用 Advisors 的后果
List<Message> history = chatMemory.get(conversationId);
List<Document> docs = vectorStore.similaritySearch(query);
String context = docs.stream().map(Document::getText).collect(joining("\n"));
String fullPrompt = "基于以下资料：\n" + context + "\n\n用户问题：" + query;
String response = chatClient.prompt().user(fullPrompt).call().content();
chatMemory.put(conversationId, query, response);
```

这是过程式代码。可读性差、难组合、难测试。

### Advisors 的解法

Advisors 本质上是一个**责任链模式**——跟 Servlet Filter、Spring AOP、Zuul Filter 是同一类东西：

```
请求进入 → [MemoryAdvisor] → [RAGAdvisor] → [SafetyAdvisor] → ChatModel → 响应出去
```

每个 Advisor 在请求到达 ChatModel 之前做前处理，在响应回来之后做后处理。多个 Advisor 可以组合成一条链，顺序可以配置。

使用 Advisors 的代码：

```java
ChatClient client = ChatClient.builder(chatModel)
    .defaultAdvisors(
        new MessageChatMemoryAdvisor(chatMemory),  // 对话记忆
        new QuestionAnswerAdvisor(vectorStore)       // RAG 检索
    )
    .build();

String answer = client.prompt()
    .user(query)
    .advisors(a -> a.param("chat_memory_conversation_id", conversationId))
    .call()
    .content();
```

代码的意图一目了然。**记忆和 RAG 不是写在调用代码里的拼凑逻辑，而是声明式的切面。**

### 为什么这个架构有价值

如果你熟悉 Spring，你会意识到一个模式：**Spring 总是在「需要横切处理的地方」插入一条拦截器链。**

| 领域 | 拦截器模式 |
|------|-----------|
| Web MVC | HandlerInterceptor |
| 安全 | Security Filter Chain |
| HTTP 调用 | ClientHttpRequestInterceptor |
| 事务 | TransactionInterceptor |
| AI 调用 | **Advisors** |

这是 Spring 团队对「如何在框架层面解决横切关注点」的一贯答案。**Advisors 的出现意味着 Spring 把 AI 调用也纳入了这套框架逻辑。** 这不是一个随意的 API 设计——它意味着 RAG、记忆、安全这些 AI 领域的横切关注点，在 Spring 的视角里和日志、事务、安全是同一种问题。

如果你曾经写过自定义的 HandlerInterceptor 或 Filter，那你就已经在理解 Advisors 了。

---

## 一次请求的完整旅程

下面这张图追踪了 `chatClient.prompt().user("你好").call().content()` 从调用到返回的全路径：

```
                        ┌──────────────┐
                        │  Controller  │
                        │  @GetMapping │
                        └──────┬───────┘
                               │ user("你好")
                               ▼
                        ┌──────────────┐
                        │  ChatClient  │  ← 构建 Prompt 对象，组装 Request
                        │  .prompt()   │
                        │  .call()     │  ← 触发执行
                        └──────┬───────┘
                               │
                               ▼
                        ┌──────────────┐
                        │  Advisors 链  │  ← RAG、记忆、安全拦截
                        │  (先处理前)   │
                        └──────┬───────┘
                               │ 增强后的 Prompt
                               ▼
                        ┌──────────────┐
                        │  ChatModel   │  ← 跨 Provider 抽象
                        │  .call()     │
                        └──────┬───────┘
                               │
                    ┌──────────┴──────────┐
                    ▼                     ▼
            ┌──────────────┐     ┌──────────────┐
            │  Ollama API  │     │  DeepSeek    │  ← 具体 Provider
            │  HTTP 调用   │     │  HTTP 调用   │
            └──────┬───────┘     └──────┬───────┘
                   │                    │
                   ▼                    ▼
            ┌──────────────┐     ┌──────────────┐
            │  LLM 模型   │     │  LLM 模型   │
            │  生成回复    │     │  生成回复    │
            └──────┬───────┘     └──────┬───────┘
                   │                    │
                   └──────────┬─────────┘
                              ▼ 响应文本
                        ┌──────────────┐
                        │  Advisors 链  │  ← 后处理（保存记忆等）
                        │  (处理后)    │
                        └──────┬───────┘
                              ▼
                        ┌──────────────┐
                        │  ChatClient  │
                        │  .content()  │
                        └──────┬───────┘
                              ▼
                         "你好，我是..."
```

这个链条的关键在于：

1. **ChatClient 本身不直接调用 API。** 它委托给 ChatModel。
2. **ChatModel 不处理横切逻辑。** 它只管格式化请求、发 HTTP、解析响应。
3. **Advisors 不直接调用模型。** 它们只做增强和拦截。
4. **每一层只做一件事，通过组合完成复杂功能。**

这就是**关注点分离**（Separation of Concerns）的教科书级实践。你可以在不修改任何已有代码的情况下，通过增加 Advisor 来添加新能力——开闭原则的体现。

---

## ChatClient 到底解决了什么问题

Spring AI 官方文档里有句话：

> Spring AI addresses the fundamental challenge of AI integration: Connecting your enterprise Data and APIs with AI Models.

翻译过来：「把你的数据和接口跟 AI 模型连起来。」

这在 Java 世界里是个非常具体的问题。

在 Python 里，你可以：

```python
response = client.chat.completions.create(
    model="gpt-4",
    messages=[{"role": "user", "content": "你好"}]
)
```

在 Java 里，没有这样的语法糖。如果你要手写调用 OpenAI API：

```java
// 手写调用 OpenAI
HttpClient client = HttpClient.newHttpClient();
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("https://api.openai.com/v1/chat/completions"))
    .header("Authorization", "Bearer " + apiKey)
    .header("Content-Type", "application/json")
    .POST(HttpRequest.BodyPublishers.ofString(
        "{\"model\":\"gpt-4\",\"messages\":[{\"role\":\"user\",\"content\":\"你好\"}]}"))
    .build();
String response = client.send(request, BodyHandlers.ofString()).body();
// 再手动解析 JSON...

// 要是想 streaming 就更麻烦了
```

这才几行代码？一次调用已经这样了，对接流式响应、函数调用、多轮对话、错误重试呢？

**Java 没有 Python 那种「随手调」的奢侈。** 所以 Spring AI 出现之前，Java 开发者在 AI 集成上一直处在「要么手写 HTTP，要么绕道 Python 写个中间层」的尴尬位置。

ChatClient 解决的，就是这个尴尬。它不是「又多了一个 AI 库」，而是**让 Java 开发者终于有了一个不掉价的 AI 调用方式。**

---

## 架构对比：Spring AI vs LangChain

如果你带着「Spring AI 就是 Java 版 LangChain」的印象来看，下面的对比会让你看到更本质的东西：

| 维度 | Spring AI | LangChain (Python) |
|------|-----------|-------------------|
| **核心抽象** | ChatClient（客户端） | Runnable（计算单元） |
| **抽象哲学** | 封装实现细节，暴露简洁 API | 暴露内部结构，方便自定义 |
| **扩展方式** | Advisors 链（AOP 风格） | Callbacks 钩子 + 自定义 Runnable |
| **组合方式** | Builder 显式组装 | `|` 运算符隐式管道 |
| **心智模型来源** | WebClient / RestTemplate | Unix 管道 / 函数式编程 |
| **关注点分离** | 分层架构（每层一个职责） | DAG 架构（每个节点一个职责） |
| **配置方式** | application.yml + DI | 代码内构造 |
| **类型安全** | 强类型（编译时） | 动态类型（运行时） |
| **适用场景** | 企业应用集成 | 原型研究/实验性项目 |
| **学习路径** | 从熟悉的 Spring 模式出发 | 从头学一套新概念 |

这表不是用来分高下的。两种设计哲学都有它的价值：

- **LangChain** 的 DAG + Pipe 模式在做复杂 Agent 编排时更有优势。你可以把 Chain 的中间结果串联起来做复杂判断，灵活性很高。
- **Spring AI** 的分层 + 依赖注入模式在**企业应用集成**场景下更有优势。你的 Service 层直接注入 ChatClient，跟注入 JdbcTemplate 一样自然。

关键是：**你用哪个舒服，取决于你从哪里来。** 我从 Spring 来，ChatClient 让我觉得 AI 集成不过如此。

---

## 第一次跑起来

配一个本地模型（用 Ollama），你就能体会到这种抽象的威力。

```bash
brew install ollama
ollama pull qwen2.5:7b
```

然后写一个 Controller：

```java
@RestController
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    @GetMapping("/chat")
    String chat(@RequestParam String message) {
        return chatClient.prompt()
            .user(message)
            .call()
            .content();
    }
}
```

启动项目，访问：

```
GET http://localhost:8080/chat?message=用一句话介绍自己
```

返回：*"我是通义千问，一个由阿里云开发的 AI 助手……"*

你把 Provider 从 Ollama 换成 DeepSeek，返回的内容变了——但你的代码里没有一个字符需要改。

### Call vs Stream

```java
// 同步
chatClient.prompt().user("你好").call().content();

// 流式
chatClient.prompt().user("你好").stream().content();  // 返回 Flux<String>
```

同步的 `call()` 返回 `String`，流式的 `stream()` 返回 `Flux<String>`。

就是 WebClient 里 `retrieve()`（Mono）和 `exchange()`（Flux）的关系——完全一样的对称设计。**「ChatClient 是 WebClient 的 AI 版本」不是比喻，是字面意思的设计同构。**

---

## 这种设计哲学的价值在哪

回到开头的问题。

ChatClient 这种设计，到底好在哪？或者说，**Spring AI 通过 ChatClient 传递了什么设计信号？**

我觉得有三点值得记住：

### 第一：降低认知成本

Python 的 AI 生态（LangChain、LlamaIndex）确实丰富，但你得接受一套全新的编程心智模型——Chain、Graph、Agent、Tool，每个概念都有自己的抽象。

Spring AI 的选择是反过来的：**维持 Spring 已有的心智模型，把 AI 概念映射进来。**

| Python AI 概念 | Spring AI 映射 |
|---------------|---------------|
| Chain | ChatClient 的链式调用 |
| Runnable | 不需要，ChatClient 本身就是 |
| Tool/Function | @Tool 注解，同 @Service |
| Callbacks | Advisors 链 |
| Agent | Advisors + Tools 的组合 |
| Memory | ChatMemoryAdvisor + Spring Cache |

作为 Java 开发者，这意味着你可以用已有的知识去理解 AI 集成。你不必变成一个 AI 工程师——你只需要知道怎么用 ChatClient。

### 第二：善用已有的架构

Advisors 链的存在说明了一个更深层的事实：**Spring 把 AI 集成纳入了它已运行 20 年的框架逻辑。**

HandlerInterceptor → SecurityFilter → TransactionInterceptor → **Advisors**。AI 不过是这条链上的最新环节。这意味着 Spring 生态里的一切——依赖注入、配置管理、可观测性、事务管理——都可以直接用在 AI 集成上，不需要额外的胶水代码。

### 第三：Java 在 AI 领域的独特价值

Java 的强类型、声明式配置、AOP 能力在 AI 集成中不是劣势，反而是优势。当你需要在一个企业级应用中稳定地集成 AI 能力——生产级别的错误处理、监控、配置管理、测试——Java 的工具链远比 Python 成熟。

ChatClient 让 Java 开发者不必在「用 Python 写 AI 代理层」和「手写 HTTP 调 AI」之间做选择。它让 AI 集成成为 Spring 应用开发的一个正常部分，而不是一个需要单独架构的子系统。

这也许是 Spring AI 对 Java 生态最大的价值：**它不是在 Java 里复刻一个 LangChain，而是让 AI 集成变得 Spring 化。**

---

## 代码仓库

见本目录下的 [code/](./code/)，包含：

- `pom.xml` — Spring Boot 4 + Spring AI 2.0.0-M8
- `Application.java` — 启动类
- `ChatController.java` — 完整版 Controller（含同步 + 流式 + 多轮对话）
- `application.yml` — 支持 Ollama / DeepSeek 两种配置

`mvn spring-boot:run` 启动，访问 `http://localhost:8080/chat?message=你好`。

---

## 下期预告

下一篇聊结构化输出——为什么 Java 强类型在 AI 集成里反而是优势。

> 当 Python 开发者还在 `json.loads(response.choices[0].message.content)` 的时候，你在用 `BeanOutputConverter` 直接把 LLM 输出映射到 POJO。而且阿里的通义千问在 JSON 结构化输出上比 GPT 还稳——有些东西，框架帮不了你，语言本身的类型系统可以。

---

> **本专栏系列**
>
> 01 — ChatClient 设计哲学（本文）
> 02 — 结构化输出：强类型的胜利
> 03 — @Tool = @Service：当 Function Calling 遇上 DI
> 04 — RAG 不是什么黑科技，它就是缓存模式
> 05 — 多 Provider 切换：Spring 的抽象层是盾还是剑
> 06 — MCP：当你的 Spring Bean 成为 AI 的工具（即将推出）
> 07 — 实战：从话务场景出发的智能客服辅助套件（即将推出）
