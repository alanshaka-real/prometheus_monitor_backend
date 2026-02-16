package com.wenmin.prometheus.module.alert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wenmin.prometheus.module.alert.entity.PromSilence;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PromSilenceMapper extends BaseMapper<PromSilence> {
}
