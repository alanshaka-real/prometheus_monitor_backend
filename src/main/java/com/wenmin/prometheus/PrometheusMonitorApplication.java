package com.wenmin.prometheus;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@MapperScan("com.wenmin.prometheus.module.*.mapper")
@EnableScheduling
public class PrometheusMonitorApplication {
    public static void main(String[] args) {
        SpringApplication.run(PrometheusMonitorApplication.class, args);
    }
}
