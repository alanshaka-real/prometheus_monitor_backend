package com.wenmin.prometheus.module.dashboard.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wenmin.prometheus.module.dashboard.entity.PromDashboard;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DashboardMapper extends BaseMapper<PromDashboard> {
}
