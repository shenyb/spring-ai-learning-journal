# Spring AI Learning Journal

一个 Java 后端开发者的 Spring AI 学习之旅。

## 动机

做了十年 Java 后端（Spring Boot + MyBatis + K8s），现在开始探索 Spring AI。原因：

- **Java 生态的 AI 浪潮正在来** —— Spring AI 2.0 GA 即将发布，Java 终于有了自己的 AI 框架
- **现有业务天然契合** —— 话务/坐席场景本身就是 AI 集成的黄金领域（智能客服摘要、坐席辅助、知识库问答）
- **Java 开发者的稀缺窗口期** —— 现在学 Spring AI 的人少，写文章、做开源都有先发优势
- **小项目/外包的种子** —— 企业级 Java AI 应用的需求会越来越多

这个仓库记录一个 Spring Boot 老手的 AI 探索视角——哪些设计是 Spring 哲学的自然延伸、哪些是全新的概念、以及怎么用 Java 的方式做 AI。

## 目录

- [01-hello-world/](./01-hello-world/) ChatClient 基础 + 多 Provider 对比
- [02-structured-output/](./02-structured-output/) 结构化输出 + Function Calling（Spring AI 最值钱的部分）
- [03-rag/](./03-rag/) RAG + 向量数据库（PGVector / ES）
- [04-project/](./04-project/) 小项目 / 开源雏形（智能客服辅助套件）
- [05-articles/](./05-articles/) 从笔记打磨出来的文章稿件

## 进度

- [ ] 01 — ChatClient 初体验 + 多 Provider 切换
- [ ] 02 — 结构化输出（LLM → POJO，告别 JSON 手工解析）
- [ ] 03 — Function Calling（@Tool 注解）
- [ ] 04 — RAG 基础（ETL Pipeline + 向量检索）
- [ ] 05 — RAG 进阶（Advisors API）
- [ ] 06 — 小项目：智能客服辅助套件
- [ ] 07 — 整理成掘金文章

## 背景

- 主力语言：Java（8 + Spring Boot + MyBatis-Plus）
- 工作领域：云平台运维（K8s）、呼叫中心后端（事件驱动架构）
- Spring 经验：资深用户，RestTemplate / WebClient / DI / AOP 信手拈来
- 学习目标：掌握 Spring AI，能写文章、能搞开源、能接小项目
