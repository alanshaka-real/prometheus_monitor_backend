package com.wenmin.prometheus.module.query.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wenmin.prometheus.module.query.entity.PromPromqlTemplate;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PromqlTemplateMapper extends BaseMapper<PromPromqlTemplate> {
}
