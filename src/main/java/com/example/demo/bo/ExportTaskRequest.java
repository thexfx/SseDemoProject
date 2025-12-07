package com.example.demo.bo;

/**
 * 文件导出任务请求
 */
public record ExportTaskRequest(
        /**
         * 用户唯一标识
         */
        String userId,

        /**
         * 导出文件类型，如 "CSV", "EXCEL"
         */
        String fileType,

        /**
         * 数据范围描述
         */
        String dataScope,

        /**
         * 总记录数（用于模拟进度）
         */
        int totalRecords
) {
    public ExportTaskRequest {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("用户ID不能为空");
        }
        if (fileType == null || fileType.isBlank()) {
            throw new IllegalArgumentException("文件类型不能为空");
        }
        if (totalRecords <= 0) {
            throw new IllegalArgumentException("总记录数必须大于0");
        }
    }
}