package com.wenmin.prometheus.module.datasource.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wenmin.prometheus.module.datasource.entity.PromInstance;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PromInstanceMapper extends BaseMapper<PromInstance> {
}
