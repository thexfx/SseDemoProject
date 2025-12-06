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
        String status,
        int currentStep,
        int totalSteps,
        String message,
        Object result
) {

    /**
     * 构建“处理中”状态的进度对象
     *
     * @param taskId      任务 ID
     * @param step        当前步骤（从 1 开始）
     * @param total       总步骤数
     * @param msg         进度描述信息
     * @return 处理中的任务进度对象
     */
    public static TaskProgress processing(String taskId, int step, int total, String msg) {
        return new TaskProgress(taskId, "processing", step, total, msg, null);
    }

    /**
     * 构建“已完成”状态的结果对象
     *
     * @param taskId 任务 ID
     * @param result 任务最终结果（可为字符串、JSON 对象等）
     * @return 完成状态的任务结果对象
     */
    public static TaskProgress completed(String taskId, Object result) {
        return new TaskProgress(taskId, "completed", -1, -1, "Task finished", result);
    }

    /**
     * 构建“失败”状态的错误对象
     *
     * @param taskId    任务 ID
     * @param errorMsg  错误描述信息
     * @return 失败状态的任务错误对象
     */
    public static TaskProgress failed(String taskId, String errorMsg) {
        return new TaskProgress(taskId, "failed", -1, -1, errorMsg, null);
    }
}
