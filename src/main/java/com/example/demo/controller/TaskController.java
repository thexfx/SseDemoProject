package com.example.demo.controller;

import com.example.demo.bo.TaskProgress;
import com.example.demo.bo.TaskRequest;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    /**
     * 提交任务，并立即返回 SSE 流用于监听进度
     */
    @GetMapping(value = "/submit", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<TaskProgress>> submitTask(@RequestBody TaskRequest request) {

        String actualTaskId = request.taskId() != null ? request.taskId() : UUID.randomUUID().toString();

        // 模拟异步任务处理过程：每步间隔 800ms，共 totalSteps 步
        return Flux.range(1, request.totalSteps())
                .delayElements(Duration.ofMillis(800))
                .map(step -> TaskProgress.processing(
                        actualTaskId,
                        step,
                        request.totalSteps(),
                        "Processing step " + step + ": " + request.description()
                ))
                .map(progress -> ServerSentEvent.<TaskProgress>builder()
                        .id(actualTaskId + "-" + progress.currentStep())
                        .event("progress")
                        .data(progress)
                        .build())
                .subscribeOn(Schedulers.boundedElastic()) // 在 I/O 线程池执行（模拟阻塞操作）
                .concatWith(Mono.fromCallable(() -> {
                    // 模拟最终结果生成（如文件路径、计算结果等）
                    String finalResult = "Result of task '" + request.description() + "' with ID: " + actualTaskId;
                    return TaskProgress.completed(actualTaskId, finalResult);
                }).map(result -> ServerSentEvent.<TaskProgress>builder()
                        .id(actualTaskId + "-done")
                        .event("complete")
                        .data(result)
                        .build()))
                .doOnError(throwable -> {
                    // 可选：错误处理
                });
    }
}