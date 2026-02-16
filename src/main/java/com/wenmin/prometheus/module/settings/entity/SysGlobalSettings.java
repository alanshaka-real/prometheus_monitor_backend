package com.wenmin.prometheus.module.settings.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@TableName("sys_global_settings")
public class SysGlobalSettings {
    @TableId(type = IdType.ASSIGN_UUID)
    private String id;
    private String settingKey;
    private String settingValue;
    private String description;
    private LocalDateTime updatedAt;
}
