package com.wenmin.prometheus.module.query.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wenmin.prometheus.module.query.entity.PromQueryHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface QueryHistoryMapper extends BaseMapper<PromQueryHistory> {
}
