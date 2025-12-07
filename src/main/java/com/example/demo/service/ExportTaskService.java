package com.example.demo.service;

import com.example.demo.bo.ExportTaskRequest;
import com.example.demo.bo.TaskProgress;
import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 导出任务服务：异步执行 + 实时进度推送（SSE）
 * <p>
 * 设计说明：
 * - 每个用户拥有一个“全局进度 Sink”，所有该用户的任务都向此 Sink 推送事件；
 * - 前端通过 GET /progress/stream?userId=xxx 建立长连接，持续接收该用户所有任务进度；
 * - 任务完成后不关闭用户 Sink，以便支持后续新任务；
 * - 用户长时间无连接时，Sink 会因无订阅者而自动丢弃事件（背压缓冲有限）；
 * - 内存中的 Sink 不主动清理（可扩展为带 TTL 的缓存）。
 */
@Service
public class ExportTaskService {

    private static final Logger log = LoggerFactory.getLogger(ExportTaskService.class);

    // 注入 Spring 配置的线程池（见 TaskExecutorConfig）
    @Resource(name = "exportTaskExecutor")
    private ThreadPoolTaskExecutor exportTaskExecutor;

    // 用户ID -> 全局进度广播 Sink（多订阅者安全）
    private final Map<String, Sinks.Many<TaskProgress>> userGlobalSinks = new ConcurrentHashMap<>();

    /**
     * 提交导出任务
     *
     * @return 任务ID
     */
    public String submitExportTask(ExportTaskRequest request) {
        String taskId = generateTaskId();
        String userId = request.userId();

        // 获取或创建该用户的全局 Sink（用于聚合所有任务进度）
        Sinks.Many<TaskProgress> userSink = userGlobalSinks.computeIfAbsent(userId, this::createUserGlobalSink);

        log.info("受理导出任务 | userId: {}, taskId: {}, fileType: {}, totalRecords: {}",
                userId, taskId, request.fileType(), request.totalRecords());

        // 异步执行任务（提交到 Spring 管理的线程池）
        exportTaskExecutor.execute(() -> processExportTask(taskId, request, userSink));

        return taskId;
    }

    /**
     * 生成唯一任务ID
     */
    private String generateTaskId() {
        return "export-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8);
    }

    /**
     * 创建用户级全局 Sink（支持多订阅者，带背压缓冲）
     */
    private Sinks.Many<TaskProgress> createUserGlobalSink(String userId) {
        log.debug("为用户 {} 创建全局进度 Sink", userId);
        return Sinks.many()
                .multicast()               // 支持多个订阅者（如多标签页）
                .onBackpressureBuffer(100); // 缓冲最多100条未消费消息，防止 OOM

    }

    /**
     * 模拟导出任务处理逻辑（在独立线程中执行）
     */
    private void processExportTask(String taskId, ExportTaskRequest request, Sinks.Many<TaskProgress> userSink) {
        String userId = request.userId();
        String fileType = request.fileType();
        int totalRecords = request.totalRecords();

        try {
            // 计算模拟步骤数（每100条记录为1步）
            int totalSteps = Math.max(1, totalRecords / 100);
            log.debug("开始处理任务 | userId: {}, taskId: {}, totalSteps: {}", userId, taskId, totalSteps);

            for (int step = 1; step <= totalSteps; step++) {
                // 模拟耗时操作
                Thread.sleep(800);

                String message = "正在导出 %s 数据（第 %d/%d 步）".formatted(fileType, step, totalSteps);
                TaskProgress progress = TaskProgress.processing(taskId, userId, step, totalSteps, message);

                // 推送进度到用户全局 Sink
                Sinks.EmitResult result = userSink.tryEmitNext(progress);
                if (result.isFailure()) {
                    log.warn("进度推送失败 | userId: {}, taskId: {}, reason: {}", userId, taskId, result);
                } else {
                    log.info("进度已推送 | userId: {}, taskId: {}, step: {}/{}", userId, taskId, step, totalSteps);
                }
            }

            // 任务成功完成
            String downloadUrl = "/download/" + taskId + "." + fileType.toLowerCase();
            TaskProgress completed = TaskProgress.completed(taskId, userId, downloadUrl);
            userSink.tryEmitNext(completed);
            log.info("任务完成 | userId: {}, taskId: {}, downloadUrl: {}", userId, taskId, downloadUrl);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            String errorMsg = "任务被中断";
            log.warn("任务中断 | userId: {}, taskId: {}", userId, taskId, e);
            userSink.tryEmitNext(TaskProgress.failed(taskId, userId, errorMsg));
        } catch (Exception e) {
            String errorMsg = "导出过程中发生异常: " + e.getMessage();
            log.error("任务执行异常 | userId: {}, taskId: {}", userId, taskId, e);
            userSink.tryEmitNext(TaskProgress.failed(taskId, userId, errorMsg));
        }
        // 注意：不调用 userSink.tryEmitComplete()！
        // 因为用户可能提交新任务，需保持 Sink 可用
    }

    /**
     * 获取指定用户的任务进度流（用于 SSE）
     * <p>
     * 返回的 Flux 会持续推送该用户的所有任务进度，直到连接断开或超时（20分钟由 Controller 控制）
     */
    public Flux<TaskProgress> getProgressStreamForUser(String userId) {
        log.info("用户 {} 开始监听任务进度流", userId);
        // 获取或创建 Sink（即使无任务，也返回空流）
        Sinks.Many<TaskProgress> sink = userGlobalSinks.computeIfAbsent(userId, this::createUserGlobalSink);
        return sink.asFlux();
    }
}
