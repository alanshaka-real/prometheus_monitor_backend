package com.wenmin.prometheus.module.query.vo;

import lombok.Data;

import java.util.Map;

@Data
public class ExporterInstanceVO {

    private String id;
    private String name;
    private String address;
    private String status;
    private Map<String, String> labels;
}
