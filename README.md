# Spring AI 探秘

**一个 Java 老手的 AI 第一线**

> 这不是教程。这是一个做了十年 Spring Boot 的 Java 程序员，用他的框架直觉审视 Spring AI 的专栏。
>
> 每篇一个独特的 Java 视角——不翻译文档、不跑流水账 Demo、不堆砌术语。

## 为什么写这个专栏

2026 年，AI 已经不是一个「要不要用」的问题，而是「用什么姿势用」。

Python 生态有 LangChain、LlamaIndex、AutoGen，跑得飞快。但 Java 世界也有 Spring AI 了——而且它带来的不是「能用」，是 **用 Spring 的方式用 AI**。

作为一个从 RestTemplate 年代走过来的 Spring 开发者，我看到 ChatClient 的第一反应不是「哦又一个 AI 库」，而是「这不就是 WebClient 的 AI 版本吗」。

这个专栏就是记录这种「原来如此」的瞬间。

## 适合谁读

- 有 Java/Spring Boot 基础，想了解 AI 集成但不打算转 Python
- 工作中遇到了「要不要上 AI」的场景，想看看 Java 这边有什么方案
- 写过一些 AI Demo，但觉得 Python 那套在 Java 项目里水土不服

## 文章列表

| # | 状态 | 标题 |
|---|------|------|
| 01 | ✅ 已发布 | [ChatClient 设计哲学：从 RestTemplate 到 AI](articles/01-chatclient-philosophy/article.md) |
| 02 | 📝 写作中 | 结构化输出：为什么 Java 做 AI 比 Python 更舒服 |
| 03 | ⏳ 待写 | @Tool = @Service：当 Function Calling 遇上 DI |
| 04 | ⏳ 待写 | RAG 不是什么黑科技，它就是缓存模式 |
| 05 | ⏳ 待写 | 多 Provider 切换：Spring 的抽象层是盾还是剑 |
| 06 | ⏳ 待写 | MCP：当你的 Spring Bean 成为 AI 的工具 |
| 07 | ⏳ 待写 | 实战：从话务场景出发的智能客服辅助套件 |

## 配套代码

每篇文章的 `code/` 目录下有一个完整的可运行项目，按文章中的说明即可跑起来。

文章里的代码块会截取核心片段，`code/` 里是完整可编译的版本。

## 在哪里看

- [掘金](https://juejin.cn/user/...) ← TODO: 发布后补链接
- 这个仓库本身也是发布源，每篇文章可独立阅读
