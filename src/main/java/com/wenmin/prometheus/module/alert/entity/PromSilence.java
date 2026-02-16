package com.wenmin.prometheus.module.alert.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@TableName(value = "prom_silence", autoResultMap = true)
public class PromSilence {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<Map<String, Object>> matchers;

    private LocalDateTime startsAt;

    private LocalDateTime endsAt;

    private String createdBy;

    private String comment;

    private String status;

    private LocalDateTime createdAt;
}
