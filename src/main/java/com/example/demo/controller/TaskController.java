package com.example.demo.controller;

import com.example.demo.bo.ExportTaskRequest;
import com.example.demo.bo.TaskProgress;
import com.example.demo.service.ExportTaskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private static final Logger log = LoggerFactory.getLogger(TaskController.class);

    @Autowired
    private ExportTaskService taskService;

    /**
     * 提交文件导出任务
     * POST /api/tasks/export
     */
    @PostMapping("/export")
    public Mono<TaskSubmissionResponse> submitExportTask(@RequestBody ExportTaskRequest request) {
        // 提交导出任务到服务层处理
        String taskId = taskService.submitExportTask(request);
        log.info("任务受理成功: userId={}, taskId={}", request.userId(), taskId);
        // 返回任务提交成功的响应信息
        return Mono.just(new TaskSubmissionResponse(taskId, "任务已受理"));
    }

    /**
     * 监听用户任务进度（GET + query param）
     * 兼容浏览器 EventSource
     */
    @GetMapping(value = "/progress/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<TaskProgress>> streamUserTaskProgress(
            @RequestParam String userId) {

        if (userId == null || userId.isBlank()) {
            return Flux.error(new IllegalArgumentException("userId 不能为空"));
        }

        log.info("用户 {} 开始监听任务进度流（SSE 连接建立）", userId);

        return taskService.getProgressStreamForUser(userId)
                .map(progress -> ServerSentEvent.<TaskProgress>builder()
                        .id(progress.taskId() + "-" + System.currentTimeMillis())
                        .event(getEventName(progress.status()))
                        .data(progress)
                        .build())
                // ⏱️ 自动断开：20 分钟超时
                .take(Duration.ofMinutes(5))
                .doOnCancel(() -> log.info("用户 {} 的 SSE 连接已断开", userId));
    }

    private String getEventName(String status) {
        return switch (status) {
            case "completed" -> "complete";
            case "failed" -> "error";
            default -> "progress";
        };
    }

    /**
     * 任务提交响应
     */
    public record TaskSubmissionResponse(String taskId, String message) {}


}