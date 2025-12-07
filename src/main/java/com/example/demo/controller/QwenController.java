package com.example.demo.controller;

import com.example.demo.service.QwenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/qwen")
public class QwenController {

    private static final Logger log = LoggerFactory.getLogger(QwenController.class);

    private final QwenService qwenService;

    public QwenController(QwenService qwenService) {
        this.qwenService = qwenService;
    }

    @PostMapping("/ask")
    public AnswerResponse ask(@RequestBody QuestionRequest request) throws Exception {
        String answer = qwenService.askQuestionSync(request.question());
        return new AnswerResponse(answer);
    }

    /**
     * GET 接口，用于流式获取 Qwen 的回答
     *
     * @param question 用户的问题
     * @return Flux<ServerSentEvent<String>> SSE 流
     */
    @GetMapping(value = "/ask/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> streamAnswer(@RequestParam String question) {
        log.info("收到流式问答请求，问题: {}", question);

        // 参数校验
        if (question == null || question.trim().isEmpty()) {
            log.warn("请求参数 'question' 为空");
            // 返回一个错误事件流
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("问题不能为空")
                    .build());
        }

        // 调用 Service 层获取流式响应
        return qwenService.askQuestionStreamSSE(question);
    }

    public record QuestionRequest(String question) {}
    public record AnswerResponse(String answer) {}
}
