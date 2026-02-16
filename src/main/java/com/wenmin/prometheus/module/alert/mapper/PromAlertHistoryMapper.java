package com.wenmin.prometheus.module.alert.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wenmin.prometheus.module.alert.entity.PromAlertHistory;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PromAlertHistoryMapper extends BaseMapper<PromAlertHistory> {
}
