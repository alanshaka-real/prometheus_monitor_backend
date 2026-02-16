package com.wenmin.prometheus.module.cluster.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wenmin.prometheus.module.cluster.entity.PromCluster;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface PromClusterMapper extends BaseMapper<PromCluster> {
}
