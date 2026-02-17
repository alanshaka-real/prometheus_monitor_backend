package com.wenmin.prometheus.module.notification.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wenmin.prometheus.module.chat.entity.ChatConversation;
import com.wenmin.prometheus.module.chat.entity.ChatConversationMember;
import com.wenmin.prometheus.module.chat.entity.ChatMessage;
import com.wenmin.prometheus.module.chat.mapper.ChatConversationMapper;
import com.wenmin.prometheus.module.chat.mapper.ChatConversationMemberMapper;
import com.wenmin.prometheus.module.chat.mapper.ChatMessageMapper;
import com.wenmin.prometheus.module.auth.entity.SysUser;
import com.wenmin.prometheus.module.auth.mapper.SysUserMapper;
import com.wenmin.prometheus.module.notification.entity.SysNotification;
import com.wenmin.prometheus.module.notification.mapper.SysNotificationMapper;
import com.wenmin.prometheus.module.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final SysNotificationMapper notificationMapper;
    private final ChatConversationMemberMapper memberMapper;
    private final ChatConversationMapper conversationMapper;
    private final ChatMessageMapper messageMapper;
    private final SysUserMapper userMapper;

    @Override
    public Map<String, Object> getCounts(String userId) {
        long noticeUnread = notificationMapper.selectCount(
                new LambdaQueryWrapper<SysNotification>()
                        .eq(SysNotification::getUserId, userId)
                        .eq(SysNotification::getCategory, "notice")
                        .eq(SysNotification::getIsRead, 0));

        long pendingUnread = notificationMapper.selectCount(
                new LambdaQueryWrapper<SysNotification>()
                        .eq(SysNotification::getUserId, userId)
                        .eq(SysNotification::getCategory, "pending")
                        .eq(SysNotification::getIsHandled, 0));

        long messageUnread = countUnreadChatMessages(userId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("noticeUnread", noticeUnread);
        result.put("messageUnread", messageUnread);
        result.put("pendingUnread", pendingUnread);
        result.put("total", noticeUnread + messageUnread + pendingUnread);
        return result;
    }

    @Override
    public Map<String, Object> listNotices(String userId, Integer page, Integer pageSize) {
        Page<SysNotification> p = new Page<>(page, pageSize);
        notificationMapper.selectPage(p,
                new LambdaQueryWrapper<SysNotification>()
                        .eq(SysNotification::getUserId, userId)
                        .eq(SysNotification::getCategory, "notice")
                        .orderByDesc(SysNotification::getCreatedAt));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", p.getRecords());
        result.put("total", p.getTotal());
        return result;
    }

    @Override
    public Map<String, Object> listChatMessageSummaries(String userId) {
        // Get all conversations the user is a member of
        List<ChatConversationMember> memberships = memberMapper.selectList(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getUserId, userId));

        List<Map<String, Object>> summaries = new ArrayList<>();
        for (ChatConversationMember member : memberships) {
            ChatConversation conv = conversationMapper.selectById(member.getConversationId());
            if (conv == null || conv.getLastMessageId() == null) continue;

            // Count unread messages
            long unread = 0;
            if (member.getLastReadMessageId() == null) {
                // Never read: all messages are unread
                unread = messageMapper.selectCount(
                        new LambdaQueryWrapper<ChatMessage>()
                                .eq(ChatMessage::getConversationId, conv.getId()));
            } else {
                // Count messages after the last read message
                ChatMessage lastReadMsg = messageMapper.selectById(member.getLastReadMessageId());
                if (lastReadMsg != null) {
                    unread = messageMapper.selectCount(
                            new LambdaQueryWrapper<ChatMessage>()
                                    .eq(ChatMessage::getConversationId, conv.getId())
                                    .gt(ChatMessage::getCreatedAt, lastReadMsg.getCreatedAt()));
                }
            }

            if (unread == 0) continue;

            ChatMessage lastMsg = messageMapper.selectById(conv.getLastMessageId());
            String displayName = conv.getName();
            String avatar = conv.getAvatar();

            if ("private".equals(conv.getType())) {
                // Get the other user's info
                List<ChatConversationMember> members = memberMapper.selectList(
                        new LambdaQueryWrapper<ChatConversationMember>()
                                .eq(ChatConversationMember::getConversationId, conv.getId())
                                .ne(ChatConversationMember::getUserId, userId));
                if (!members.isEmpty()) {
                    SysUser otherUser = userMapper.selectById(members.get(0).getUserId());
                    if (otherUser != null) {
                        displayName = otherUser.getNickName() != null ? otherUser.getNickName() : otherUser.getUsername();
                        avatar = otherUser.getAvatar();
                    }
                }
            }

            Map<String, Object> summary = new LinkedHashMap<>();
            summary.put("conversationId", conv.getId());
            summary.put("name", displayName);
            summary.put("avatar", avatar != null ? avatar : "");
            summary.put("lastMessage", lastMsg != null ? lastMsg.getContent() : "");
            summary.put("lastMessageTime", conv.getLastMessageTime() != null ? conv.getLastMessageTime().toString() : "");
            summary.put("unread", unread);
            summaries.add(summary);
        }

        // Sort by unread count descending
        summaries.sort((a, b) -> Long.compare((long) b.get("unread"), (long) a.get("unread")));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", summaries);
        return result;
    }

    @Override
    public Map<String, Object> listPending(String userId, Integer page, Integer pageSize) {
        Page<SysNotification> p = new Page<>(page, pageSize);
        notificationMapper.selectPage(p,
                new LambdaQueryWrapper<SysNotification>()
                        .eq(SysNotification::getUserId, userId)
                        .eq(SysNotification::getCategory, "pending")
                        .orderByDesc(SysNotification::getCreatedAt));
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", p.getRecords());
        result.put("total", p.getTotal());
        return result;
    }

    @Override
    public void markRead(String userId, String id) {
        notificationMapper.update(null,
                new LambdaUpdateWrapper<SysNotification>()
                        .eq(SysNotification::getId, id)
                        .eq(SysNotification::getUserId, userId)
                        .set(SysNotification::getIsRead, 1)
                        .set(SysNotification::getReadAt, LocalDateTime.now()));
    }

    @Override
    public void markAllRead(String userId, String category) {
        notificationMapper.update(null,
                new LambdaUpdateWrapper<SysNotification>()
                        .eq(SysNotification::getUserId, userId)
                        .eq(SysNotification::getCategory, category)
                        .eq(SysNotification::getIsRead, 0)
                        .set(SysNotification::getIsRead, 1)
                        .set(SysNotification::getReadAt, LocalDateTime.now()));
    }

    @Override
    public void handlePending(String userId, String id) {
        notificationMapper.update(null,
                new LambdaUpdateWrapper<SysNotification>()
                        .eq(SysNotification::getId, id)
                        .eq(SysNotification::getUserId, userId)
                        .set(SysNotification::getIsHandled, 1)
                        .set(SysNotification::getIsRead, 1)
                        .set(SysNotification::getReadAt, LocalDateTime.now()));
    }

    private long countUnreadChatMessages(String userId) {
        List<ChatConversationMember> memberships = memberMapper.selectList(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getUserId, userId));
        long total = 0;
        for (ChatConversationMember member : memberships) {
            ChatConversation conv = conversationMapper.selectById(member.getConversationId());
            if (conv == null || conv.getLastMessageId() == null) continue;

            if (member.getLastReadMessageId() == null) {
                total += messageMapper.selectCount(
                        new LambdaQueryWrapper<ChatMessage>()
                                .eq(ChatMessage::getConversationId, conv.getId()));
            } else {
                ChatMessage lastReadMsg = messageMapper.selectById(member.getLastReadMessageId());
                if (lastReadMsg != null) {
                    total += messageMapper.selectCount(
                            new LambdaQueryWrapper<ChatMessage>()
                                    .eq(ChatMessage::getConversationId, conv.getId())
                                    .gt(ChatMessage::getCreatedAt, lastReadMsg.getCreatedAt()));
                }
            }
        }
        return total;
    }
}
