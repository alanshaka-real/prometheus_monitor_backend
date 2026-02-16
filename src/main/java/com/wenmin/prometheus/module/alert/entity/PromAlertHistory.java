package com.wenmin.prometheus.module.alert.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("prom_alert_history")
public class PromAlertHistory {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String alertName;

    private String severity;

    private String status;

    private String instance;

    private String value;

    private LocalDateTime startsAt;

    private LocalDateTime endsAt;

    private String duration;

    private String handledBy;

    private LocalDateTime handledAt;

    private String remark;

    private LocalDateTime createdAt;
}
