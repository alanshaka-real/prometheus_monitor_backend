package com.wenmin.prometheus;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.wenmin.prometheus.module.*.mapper")
public class PrometheusMonitorApplication {
    public static void main(String[] args) {
        SpringApplication.run(PrometheusMonitorApplication.class, args);
    }
}
