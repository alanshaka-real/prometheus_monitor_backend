package com.wenmin.prometheus.module.query.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("prom_query_history")
public class PromQueryHistory {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String query;

    private LocalDateTime executedAt;

    private Double duration;

    private Integer resultCount;

    private Boolean favorite;

    private String userId;
}
