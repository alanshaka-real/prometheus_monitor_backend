package com.wenmin.prometheus.module.dashboard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wenmin.prometheus.module.dashboard.entity.PromDashboardTemplate;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DashboardTemplateMapper extends BaseMapper<PromDashboardTemplate> {
}
