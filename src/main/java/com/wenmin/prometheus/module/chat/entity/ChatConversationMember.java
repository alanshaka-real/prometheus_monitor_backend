package com.wenmin.prometheus.module.chat.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("chat_conversation_member")
public class ChatConversationMember {

    @TableId(type = IdType.ASSIGN_UUID)
    private String id;

    private String conversationId;

    private String userId;

    private String role;

    private String lastReadMessageId;

    private LocalDateTime lastReadTime;

    private Integer muted;

    private LocalDateTime joinedAt;
}
