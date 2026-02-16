package com.wenmin.prometheus.module.settings.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wenmin.prometheus.module.settings.entity.SysGlobalSettings;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface GlobalSettingsMapper extends BaseMapper<SysGlobalSettings> {
}
