package com.wenmin.prometheus.module.distribute.vo;

import lombok.Data;

@Data
public class ComponentDetectVO {
    private String name;           // 组件标识: prometheus, node_exporter, alertmanager, blackbox_exporter, grafana, pushgateway
    private String displayName;    // 显示名: Prometheus Server, Node Exporter, ...
    private boolean installed;     // 是否已安装
    private String installPath;    // 安装路径
    private boolean serviceExists; // systemd service 是否存在
    private String runningStatus;  // running / stopped / unknown
    private String version;        // 版本号
    private Integer port;          // 监听端口
}
