package com.wenmin.prometheus.module.datasource.vo;

import lombok.Data;

import java.util.List;

@Data
public class BatchCreateResultVO {
    private int total;
    private int created;
    private int skipped;
    private List<String> skippedTargets;
}
