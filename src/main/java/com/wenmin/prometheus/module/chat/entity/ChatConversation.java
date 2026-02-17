package com.wenmin.prometheus.module.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_conversation")
public class ChatConversation {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String type;

    private String name;

    private String avatar;

    private String ownerId;

    private String lastMessageId;

    private LocalDateTime lastMessageTime;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @TableLogic
    private Integer deleted;
}
