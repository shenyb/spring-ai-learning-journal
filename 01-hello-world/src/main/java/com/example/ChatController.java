package com.example;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
public class ChatController {

    private final ChatClient chatClient;

    public ChatController(ChatClient.Builder builder) {
        // TODO: ChatClient 的构建方式有两种
        // 1. 不传 system prompt 默认——用 builder.build()
        // 2. 传一个默认 system prompt——用 builder.defaultSystem("你是...").build()
        // 试试两种都跑一下，看响应有什么不同
        this.chatClient = builder.build();
    }

    @GetMapping("/chat")
    String chat(@RequestParam(defaultValue = "用一句话介绍你自己") String message) {
        // TODO: 调用 chatClient.prompt().user(message).call().content()
        // 把返回值 return 出去
        return "";
    }

    @GetMapping(value = "/chat/stream", produces = "text/event-stream")
    Flux<String> chatStream(@RequestParam(defaultValue = "用50个字介绍你自己") String message) {
        // TODO: 和上面不同，这里用 stream() 而不是 call()
        // chatClient.prompt().user(message).stream().content()
        // 返回 Flux<String>，浏览器访问 /chat/stream 看看效果
        return Flux.empty();
    }

    // ===== 拓展练习（选做）=====
    // 1. 加一个 POST 接口 /chat/poem，传 topic 参数，返回一首该主题的四行诗
    // 2. 对比 call() 和 stream() 的耗时差异（在浏览器和 curl 里感受一下）
}
