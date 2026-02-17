package com.wenmin.prometheus.module.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wenmin.prometheus.module.chat.entity.ChatConversationMember;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ChatConversationMemberMapper extends BaseMapper<ChatConversationMember> {
}
