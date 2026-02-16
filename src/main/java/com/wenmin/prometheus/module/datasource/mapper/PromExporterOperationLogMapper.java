package com.wenmin.prometheus.module.datasource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wenmin.prometheus.module.datasource.entity.PromExporterOperationLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PromExporterOperationLogMapper extends BaseMapper<PromExporterOperationLog> {
}
