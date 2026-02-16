package com.wenmin.prometheus.module.cluster.dto;

import lombok.Data;

@Data
public class ClusterDTO {

    private String name;
    private String description;
    private String region;
    private String instanceId;
}
