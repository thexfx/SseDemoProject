package com.example.demo.bo;

/**
 * 任务提交请求对象（DTO）
 * <p>
 * 用于客户端通过 POST 请求向服务端提交一个异步任务的参数。
 * </p>
 *
 * @param taskId      可选的任务 ID。若未提供，服务端将自动生成唯一 ID。
 * @param description 任务描述信息，用于标识任务用途（如 "生成月度报表"）。
 * @param totalSteps  任务总步数，用于模拟进度（必须大于 0）。
 */
public record TaskRequest(
        String taskId,
        String description,
        int totalSteps
) {
    /**
     * 紧凑构造器：对输入参数进行校验
     */
    public TaskRequest {
        if (totalSteps <= 0) {
            throw new IllegalArgumentException("任务总步数（totalSteps）必须大于 0");
        }
        if (description == null || description.trim().isEmpty()) {
            throw new IllegalArgumentException("任务描述（description）不能为空");
        }
    }
}
