package com.example.demo.controller;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

@RestController
@RequestMapping("/sseApi")
public class SseController {

    /**
     * 简单 SSE 接口：每秒推送当前服务器时间
     */
    @GetMapping(value = "/time-stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamServerTime() {
        return Flux.interval(Duration.ofSeconds(1))
                .map(sequence -> LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                .map(time -> "data: 当前服务器时间: " + time + "\n\n");
    }

    /**
     * 模拟任务进度推送（例如：耗时任务的实时进度）
     */
    @GetMapping(value = "/task-progress", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> simulateTaskProgress() {
        return Flux.range(1, 10) // 从 1 到 10
                .delayElements(Duration.ofMillis(500)) // 每 500ms 发送一个
                .map(step -> String.format("data: {\"step\":%d, \"status\":\"processing\", \"percent\":%d}\n\n", step, step * 10));
    }

    /**
     * 返回普通 JSON（非流式），用于对比
     */
    @GetMapping("/hello")
    public String hello() {
        return "Hello from WebFlux!";
    }
}
