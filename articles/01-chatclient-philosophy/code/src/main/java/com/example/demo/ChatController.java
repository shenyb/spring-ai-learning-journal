package com.example.demo;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        this.chatClient = builder
            .defaultSystem("你是一个友好的技术助手，回答简洁清晰。")
            .build();
    }

    /**
     * 同步调用——等待完整响应后返回
     * GET /chat?message=你好
     */
    @GetMapping("/chat")
    String chat(@RequestParam(defaultValue = "用一句话介绍你自己") String message) {
        return chatClient.prompt()
            .user(message)
            .call()
            .content();
    }

    /**
     * 流式调用——SSE 逐 token 推送
     * GET /chat/stream?message=你好
     */
    @GetMapping(value = "/chat/stream", produces = "text/event-stream")
    Flux<String> chatStream(@RequestParam(defaultValue = "用50字介绍你自己") String message) {
        return chatClient.prompt()
            .user(message)
            .stream()
            .content();
    }

    /**
     * 带默认 system prompt 的切换演示
     * GET /chat/poet?topic=春天
     */
    @GetMapping("/chat/poet")
    String poet(@RequestParam(defaultValue = "代码") String topic) {
        // 每次调用可以覆盖默认 system prompt
        return chatClient.prompt()
            .system("你是一个诗人，用四行诗回答问题。")
            .user("写一首关于" + topic + "的诗")
            .call()
            .content();
    }
}
