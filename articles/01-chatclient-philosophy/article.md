# 01 — ChatClient 设计哲学：从 RestTemplate 到 AI

> **一句话观点：** ChatClient 不是 Spring 的新发明，而是它 20 年来一直在做的事——把「调用远端服务」这件事抽象成开发者最舒服的姿势。

## 一个想法的引子

2015 年我写第一个 Spring Boot 项目时，调第三方 HTTP 接口用 RestTemplate。

```java
// 2015 年：RestTemplate
RestTemplate rest = new RestTemplate();
String result = rest.getForObject("http://api.example.com/data", String.class);
```

2019 年，Spring 5 推出 WebClient，我犹豫了一阵才切换——毕竟 RestTemplate 还能用。

```java
// 2019 年：WebClient（fluent 风格）
String result = WebClient.create("http://api.example.com")
    .get()
    .uri("/data")
    .retrieve()
    .bodyToMono(String.class)
    .block();
```

2026 年，我第一次写 Spring AI 代码：

```java
// 2026 年：ChatClient
String result = ChatClient.create(chatModel)
    .prompt()
    .user("用一句话介绍自己")
    .call()
    .content();
```

看到第三段代码的时候，我想的不是「又学了一个新 API」，而是——**Spring 连 AI 都统一成这套模式了？**

这种熟悉感不是巧合。它是 Spring 团队 20 年来一以贯之的设计哲学。

## 三层抽象

拆开看 ChatClient 的设计，它其实在三个层面做了抽象：

### 第一层：Builder 模式

所有能变的都交给 Builder。ChatClient 本身不可变，让你通过 Builder 配置：

```java
ChatClient client = ChatClient.builder(chatModel)
    .defaultSystem("你是一个专业的 Java 技术顾问")
    .defaultFunctions("getWeather")
    .build();
```

和 WebClient 如出一辙：

```java
WebClient client = WebClient.builder()
    .baseUrl("https://api.example.com")
    .defaultHeader("Accept", "application/json")
    .build();
```

这不只是代码风格相似——**这是同一个设计决策**：把组装和运行时分离。Builder 锁配置，运行时只负责执行。

### 第二层：Fluent API

`prompt()` → `user()` → `call()` → `content()` 这个链条，是 Spring 一贯的「流式动词链」。

多说一句：这种风格在 Python 的 AI 框架里很少见。LangChain 的链式调用更偏向「pipe 模式」——你会看到用 `|` 运算符串联组件。方式没有优劣，但习惯 Spring 的人拿起 ChatClient 几乎不需要学习曲线。

### 第三层：Provider 无关

同一个 ChatClient API，底层可以是 Ollama（跑本地模型）、DeepSeek、OpenAI、Claude。切换只需改配置，代码一行不动。

```yaml
spring:
  ai:
    chat:
      client: ollama
      ollama:
        chat:
          options:
            model: qwen2.5:7b
```

换一家：

```yaml
spring:
  ai:
    chat:
      client: deepseek
      deepseek:
        api-key: ${DEEPSEEK_API_KEY}
```

这和 `spring.datasource.url` 切换数据库的思路一模一样。**Spring 把 AI Provider 当成了一种 DataSource 来抽象。**

## ChatClient 到底解决了什么问题

读文档的时候，我注意到 Spring AI 官方的一句话：

> Spring AI addresses the fundamental challenge of AI integration: Connecting your enterprise Data and APIs with AI Models.

翻译过来：Spring AI 解决的核心问题是「把你的数据和接口跟 AI 模型连起来」。

这在 Java 世界里是个具体的问题。

在 Python 里，你可以：

```python
response = client.chat.completions.create(
    model="gpt-4",
    messages=[{"role": "user", "content": "你好"}]
)
```

在 Java 里，没有 `client.chat.completions` 这样的语法糖。如果你要调用 OpenAI API，得手写：

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

这才几行代码？调用一次 API 已经这样了，一个完整项目要对接流式响应、函数调用、多轮对话、错误重试……Java 没有 Python 那种「随手调」的奢侈。

所以 Spring AI 出现之前，Java 开发者在 AI 集成上一直处在「要么手写 HTTP，要么绕道 Python 写个中间层」的尴尬位置。

**ChatClient 解决的，就是这个尴尬。**

## 第一次跑起来

配一个本地模型（用 Ollama），你就能体会到这种抽象的威力。

先装好 Ollama：

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

有意思的是，你把 Provider 从 Ollama 换成 DeepSeek，同样的代码，返回的内容变了——但你的业务代码里没有一个字符需要改。

## Stream vs Call

ChatClient 同时支持同步和流式，API 差别只有一个方法名：

```java
// 同步
chatClient.prompt().user("你好").call().content();

// 流式
chatClient.prompt().user("你好").stream().content();
// 返回 Flux<String>
```

同步的 `call()` 返回 `String`，流式的 `stream()` 返回 `Flux<String>`。

对，就是 WebClient 里 `retrieve()` (`Mono`) 和 `exchange()` (`Flux`) 的关系，完全一样的对称设计。

这就是我说的「ChatClient 是 WebClient 的 AI 版本」——**不是比喻，是字面意思的设计同构。**

## 这种设计哲学的价值在哪

说回到开头的问题：ChatClient 这种设计，到底好在哪？

我觉得答案是：**降低了 Java 开发者进入 AI 领域的认知成本。**

Python 的 AI 生态（LangChain、LlamaIndex）确实丰富，但你得接受一套全新的编程心智模型——Chain、Graph、Agent、Tool，每个概念都有自己的抽象。

Spring AI 的选择是反过来的：**维持 Spring 已有的心智模型，把 AI 概念映射进来。**

- Chain → 不需要，ChatClient 的链式调用就是 Chain
- Tool → @Tool 注解，和 @Service 同源
- Agent → 不需要 Agent 框架，Spring 的 Advisors API 做同样的事
- Memory → Spring 已经有一整套缓存/会话管理方案

作为 Java 开发者，这意味着你可以用已有的知识去理解 AI 集成。你不必从一个 Web 开发者变成一个 AI 工程师——你只需要知道怎么用 ChatClient。

这可能是 Spring AI 对 Java 生态最大的价值：它不是在 Java 里复刻一个 LangChain，而是让 AI 集成变得 **Spring 化**。

---

## 代码仓库

见本目录下的 [code/](./code/)，包含：

- `pom.xml` — Spring Boot 4 + Spring AI 2.0.0-M8
- `Application.java` — 启动类
- `ChatController.java` — 完整版 Controller（含同步 + 流式）
- `application.yml` — 支持 Ollama / DeepSeek 两种配置

`mvn spring-boot:run` 即可启动，访问 `http://localhost:8080/chat?message=你好` 查看效果。

---

## 下期预告

下一篇聊结构化输出——为什么 Java 强类型在 AI 集成里反而是优势。

> 当 Python 开发者还在做 `json.loads(response.choices[0].message.content)` 的时候，你已经在用 `BeanOutputConverter` 直接把 LLM 输出映射到 POJO 上了。
