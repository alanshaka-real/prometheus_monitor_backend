package com.wenmin.prometheus.module.permission.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wenmin.prometheus.module.permission.entity.SysAuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AuditLogMapper extends BaseMapper<SysAuditLog> {
}
