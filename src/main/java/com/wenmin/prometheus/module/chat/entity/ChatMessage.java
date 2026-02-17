package com.wenmin.prometheus.module.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_message")
public class ChatMessage {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String conversationId;

    private String senderId;

    private String content;

    private String type;

    private String replyToId;

    private LocalDateTime createdAt;

    @TableLogic
    private Integer deleted;
}
