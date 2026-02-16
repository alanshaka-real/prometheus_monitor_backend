package com.wenmin.prometheus.module.cluster.dto;

import lombok.Data;

import java.util.Map;

@Data
public class DiscoveredNode {

    private String instance;
    private String ip;
    private String hostname;
    private String job;
    private String status;
    private Map<String, String> labels;
    private boolean alreadyRegistered;
}
