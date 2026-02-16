package com.wenmin.prometheus.module.workspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wenmin.prometheus.module.workspace.entity.PromWorkspaceDashboard;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkspaceDashboardMapper extends BaseMapper<PromWorkspaceDashboard> {
}
