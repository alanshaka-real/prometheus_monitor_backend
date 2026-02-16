package com.wenmin.prometheus.module.cluster.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wenmin.prometheus.module.cluster.entity.PromClusterNode;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PromClusterNodeMapper extends BaseMapper<PromClusterNode> {
}
