package com.example.demo.bo;

import lombok.Data;

/**
 * 任务进度与结果通知对象（DTO）
 * <p>
 * 用于通过 SSE（Server-Sent Events）向客户端推送任务的实时状态，
 * 包括处理中、完成或失败等阶段。
 * </p>
 *
 * @param taskId      任务唯一标识符，用于前端关联具体任务。
 * @param status      任务当前状态：
 *                    - "processing"：正在处理中
 *                    - "completed"：已成功完成
 *                    - "failed"：处理失败
 * @param currentStep 当前执行到的步骤编号（从 1 开始）；
 *                    若状态为 completed/failed，则为 -1。
 * @param totalSteps  任务总步数；若状态为 completed/failed，则为 -1。
 * @param message     人类可读的状态描述信息（如 "正在处理第 3 步"）。
 * @param result      任务最终结果（仅在 status = "completed" 时有效）；
 *                    其他状态下为 null。
 */
public record TaskProgress(
        String taskId,
        String userId,      // ← 新增
        String status,      // processing / completed / failed
        int currentStep,
        int totalSteps,
        String message,
        Object result
) {

    public static TaskProgress processing(String taskId, String userId, int step, int total, String msg) {
        return new TaskProgress(taskId, userId, "processing", step, total, msg, null);
    }

    public static TaskProgress completed(String taskId, String userId, Object result) {
        return new TaskProgress(taskId, userId, "completed", -1, -1, "导出完成", result);
    }

    public static TaskProgress failed(String taskId, String userId, String errorMsg) {
        return new TaskProgress(taskId, userId, "failed", -1, -1, errorMsg, null);
    }
}
