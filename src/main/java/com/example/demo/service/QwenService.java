package com.example.demo.service;


import com.alibaba.dashscope.aigc.generation.Generation;
import com.alibaba.dashscope.aigc.generation.GenerationParam;
import com.alibaba.dashscope.aigc.generation.GenerationResult;
import com.alibaba.dashscope.common.Message;
import com.alibaba.dashscope.common.Role;
import com.alibaba.dashscope.exception.ApiException;
import com.alibaba.dashscope.exception.InputRequiredException;
import com.alibaba.dashscope.exception.NoApiKeyException;
import com.alibaba.dashscope.protocol.Protocol;
import com.alibaba.dashscope.utils.JsonUtils;
import io.reactivex.Flowable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.adapter.rxjava.RxJava2Adapter;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@Service
public class QwenService {

    private static final Logger log = LoggerFactory.getLogger(QwenService.class);

    @Value("${dashscope.api-key}")
    private String apiKey;

    private final String apiUrl = "https://dashscope.aliyuncs.com/api/v1"; // 北京地域

    private final Generation gen = new Generation(Protocol.HTTP.getValue(), apiUrl);

    /**
     * 同步调用 Qwen Plus/Max
     */
    public String askQuestionSync(String question) throws Exception {
        List<Message> messages = Arrays.asList(
                Message.builder()
                        .role(Role.SYSTEM.getValue())
                        .content("You are a helpful assistant.")
                        .build(),
                Message.builder()
                        .role(Role.USER.getValue())
                        .content(question)
                        .build()
        );

        GenerationParam param = GenerationParam.builder()
                .apiKey(apiKey)
                .model("qwen3-max") // 或 qwen-max, qwen3-max
                .messages(messages)
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .build();

        GenerationResult result = gen.call(param);
        return result.getOutput().getChoices().get(0).getMessage().getContent();
    }

    /**
     * 流式调用 Qwen Plus/Max 模型，返回 Flux<ServerSentEvent<String>>
     *
     * @param question 用户提出的问题
     * @return Flux 流，包含 SSE 事件，每个事件的数据部分是模型返回的文本片段
     */
    public Flux<ServerSentEvent<String>> askQuestionStreamSSE(String question) {
        log.info("开始向 Qwen 发起流式请求，问题: {}", question);

        // 1. 构建对话消息
        List<Message> messages = Arrays.asList(
                Message.builder()
                        .role(Role.SYSTEM.getValue())
                        .content("You are a helpful assistant.")
                        .build(),
                Message.builder()
                        .role(Role.USER.getValue())
                        .content(question)
                        .build()
        );

        // 2. 构建请求参数
        GenerationParam param = GenerationParam.builder()
                .apiKey(apiKey)
                .model("qwen3-max") // 可替换为 qwen-max, qwen3-max
                .messages(messages) // 可扩展为多轮对话
                .resultFormat(GenerationParam.ResultFormat.MESSAGE)
                .incrementalOutput(true) // 启用增量输出以获得流式体验
                .build();

        // 3. 发起流式调用，得到 RxJava 的 Flowable
        Flowable<GenerationResult> flowableResult;
        try {
            // SDK 调用可能会抛出初始化异常
            flowableResult = gen.streamCall(param);
        } catch (NoApiKeyException | InputRequiredException e) {
            log.error("调用 DashScope SDK 失败，参数错误: ", e);
            // 返回一个包含错误信息的单事件流
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("初始化错误: " + e.getMessage())
                    .build());
        } catch (Exception e) {
            log.error("调用 DashScope SDK 时发生未知异常: ", e);
            return Flux.just(ServerSentEvent.<String>builder()
                    .event("error")
                    .data("请求初始化失败: " + e.getMessage())
                    .build());
        }

        // 4. 将 RxJava Flowable 转换为 Reactor Flux<ServerSentEvent<String>>
        //    并处理 onNext, onError, onComplete 事件
        return RxJava2Adapter.flowableToFlux(flowableResult)
                .map(generationResult -> {
                    // 处理每个接收到的响应块
                    try {
                        String content = generationResult.getOutput().getChoices().get(0).getMessage().getContent();
                        log.debug("收到模型响应片段: {}", content);
                        // 构造 SSE 事件，数据为内容片段
                        return ServerSentEvent.<String>builder()
                                .event("message") // 可自定义事件名
                                .data(content)
                                .build();
                    } catch (Exception e) {
                        log.error("处理模型响应时出错: ", e);
                        return ServerSentEvent.<String>builder()
                                .event("error")
                                .data("响应处理错误: " + e.getMessage())
                                .build();
                    }
                })
                .doOnComplete(() -> {
                    // 流完成时的日志
                    log.info("Qwen 流式响应结束");
                    // 注意：这里无法轻易访问到最后一个 chunk 的 usage 信息，
                    // 因为 onComplete 和最后一个 onNext 是分开的。
                    // 如果需要 usage，可以在 onNext 中判断 finishReason 并单独发送一个 SSE 事件。
                })
                .doOnError(error -> {
                    // 流发生错误时的日志
                    log.error("Qwen 流式响应出错: ", error);
                    // 注：错误信息也会通过 map 中的 error 事件发送给客户端
                });
        // 注意：原示例中的 CountDownLatch 在 Reactor/WebFlux 模型下是不需要的，
        // 生命周期由框架管理。
    }
}
