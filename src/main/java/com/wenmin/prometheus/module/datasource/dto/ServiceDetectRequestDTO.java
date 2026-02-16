package com.wenmin.prometheus.module.datasource.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class ServiceDetectRequestDTO {
    /** 要探测的服务类型及端口，Key=服务标识(如 node_exporter)，Value=端口号 */
    private Map<String, Integer> serviceTypes;
    /** 可选：指定机器 ID 列表，为空则探测所有机器 */
    private List<String> machineIds;
}
