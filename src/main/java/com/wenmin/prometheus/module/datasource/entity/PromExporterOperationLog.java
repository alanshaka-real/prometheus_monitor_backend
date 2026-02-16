package com.wenmin.prometheus.module.datasource.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("prom_exporter_operation_log")
public class PromExporterOperationLog {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String exporterId;

    private String operation;

    private String operator;

    private Integer success;

    private String message;

    private String detail;

    private LocalDateTime createdAt;
}
