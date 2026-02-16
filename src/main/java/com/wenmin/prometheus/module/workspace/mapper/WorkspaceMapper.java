package com.wenmin.prometheus.module.workspace.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wenmin.prometheus.module.workspace.entity.PromWorkspace;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WorkspaceMapper extends BaseMapper<PromWorkspace> {
}
