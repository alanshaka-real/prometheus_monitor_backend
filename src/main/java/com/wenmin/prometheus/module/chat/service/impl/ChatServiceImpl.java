package com.wenmin.prometheus.module.chat.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wenmin.prometheus.module.auth.entity.SysUser;
import com.wenmin.prometheus.module.auth.mapper.SysUserMapper;
import com.wenmin.prometheus.module.chat.entity.ChatConversation;
import com.wenmin.prometheus.module.chat.entity.ChatConversationMember;
import com.wenmin.prometheus.module.chat.entity.ChatMessage;
import com.wenmin.prometheus.module.chat.mapper.ChatConversationMapper;
import com.wenmin.prometheus.module.chat.mapper.ChatConversationMemberMapper;
import com.wenmin.prometheus.module.chat.mapper.ChatMessageMapper;
import com.wenmin.prometheus.module.chat.service.ChatService;
import com.wenmin.prometheus.module.notification.service.NotificationService;
import com.wenmin.prometheus.websocket.WebSocketSessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatConversationMapper conversationMapper;
    private final ChatConversationMemberMapper memberMapper;
    private final ChatMessageMapper messageMapper;
    private final SysUserMapper userMapper;
    private final WebSocketSessionManager sessionManager;
    private final NotificationService notificationService;

    @Override
    public List<Map<String, Object>> getContacts(String userId, String keyword) {
        LambdaQueryWrapper<SysUser> wrapper = new LambdaQueryWrapper<SysUser>()
                .ne(SysUser::getId, userId)
                .eq(SysUser::getStatus, "active");
        if (keyword != null && !keyword.isBlank()) {
            wrapper.and(w -> w
                    .like(SysUser::getUsername, keyword)
                    .or().like(SysUser::getNickName, keyword)
                    .or().like(SysUser::getRealName, keyword));
        }
        List<SysUser> users = userMapper.selectList(wrapper);
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysUser user : users) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", user.getId());
            map.put("username", user.getUsername());
            map.put("nickName", user.getNickName());
            map.put("realName", user.getRealName());
            map.put("avatar", user.getAvatar() != null ? user.getAvatar() : "");
            map.put("online", sessionManager.isOnline(user.getId()));
            result.add(map);
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> getConversations(String userId) {
        List<ChatConversationMember> memberships = memberMapper.selectList(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getUserId, userId));

        List<Map<String, Object>> result = new ArrayList<>();
        for (ChatConversationMember member : memberships) {
            ChatConversation conv = conversationMapper.selectById(member.getConversationId());
            if (conv == null) continue;

            Map<String, Object> map = buildConversationVO(conv, member, userId);
            result.add(map);
        }

        // Sort by lastMessageTime descending
        result.sort((a, b) -> {
            String ta = (String) a.get("lastMessageTime");
            String tb = (String) b.get("lastMessageTime");
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });

        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> createConversation(String userId, String type, String name, List<String> memberIds) {
        ChatConversation conv = new ChatConversation();
        conv.setId(UUID.randomUUID().toString());
        conv.setType(type);
        conv.setName(name != null ? name : "");
        conv.setOwnerId(userId);
        conv.setCreatedAt(LocalDateTime.now());
        conv.setUpdatedAt(LocalDateTime.now());
        conv.setDeleted(0);
        conversationMapper.insert(conv);

        // Add owner as member
        Set<String> allMembers = new LinkedHashSet<>();
        allMembers.add(userId);
        if (memberIds != null) allMembers.addAll(memberIds);

        for (String memberId : allMembers) {
            ChatConversationMember member = new ChatConversationMember();
            member.setId(UUID.randomUUID().toString());
            member.setConversationId(conv.getId());
            member.setUserId(memberId);
            member.setRole(memberId.equals(userId) ? "owner" : "member");
            member.setMuted(0);
            member.setJoinedAt(LocalDateTime.now());
            memberMapper.insert(member);
        }

        // Build and return the conversation VO
        ChatConversationMember myMember = memberMapper.selectOne(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getConversationId, conv.getId())
                        .eq(ChatConversationMember::getUserId, userId));

        return buildConversationVO(conv, myMember, userId);
    }

    @Override
    public Map<String, Object> getMessages(String conversationId, String userId, Integer page, Integer pageSize) {
        // Verify user is a member
        ChatConversationMember member = memberMapper.selectOne(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getConversationId, conversationId)
                        .eq(ChatConversationMember::getUserId, userId));
        if (member == null) {
            return Map.of("list", List.of(), "total", 0);
        }

        Page<ChatMessage> p = new Page<>(page, pageSize);
        messageMapper.selectPage(p,
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getConversationId, conversationId)
                        .orderByDesc(ChatMessage::getCreatedAt));

        List<Map<String, Object>> messages = new ArrayList<>();
        for (ChatMessage msg : p.getRecords()) {
            messages.add(buildMessageVO(msg));
        }
        // Reverse to chronological order
        Collections.reverse(messages);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", messages);
        result.put("total", p.getTotal());
        return result;
    }

    @Override
    @Transactional
    public Map<String, Object> sendMessage(String userId, String conversationId, String content, String type) {
        return doSendMessage(userId, conversationId, content, type != null ? type : "text");
    }

    @Override
    @Transactional
    public void sendMessageViaWebSocket(String userId, String conversationId, String content) {
        doSendMessage(userId, conversationId, content, "text");
    }

    private Map<String, Object> doSendMessage(String userId, String conversationId, String content, String msgType) {
        // Verify membership
        ChatConversationMember member = memberMapper.selectOne(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getConversationId, conversationId)
                        .eq(ChatConversationMember::getUserId, userId));
        if (member == null) {
            throw new RuntimeException("Not a member of this conversation");
        }

        // Create message
        ChatMessage message = new ChatMessage();
        message.setId(UUID.randomUUID().toString());
        message.setConversationId(conversationId);
        message.setSenderId(userId);
        message.setContent(content);
        message.setType(msgType);
        message.setCreatedAt(LocalDateTime.now());
        message.setDeleted(0);
        messageMapper.insert(message);

        // Update conversation
        conversationMapper.update(null,
                new LambdaUpdateWrapper<ChatConversation>()
                        .eq(ChatConversation::getId, conversationId)
                        .set(ChatConversation::getLastMessageId, message.getId())
                        .set(ChatConversation::getLastMessageTime, message.getCreatedAt())
                        .set(ChatConversation::getUpdatedAt, LocalDateTime.now()));

        // Auto-mark as read for sender
        memberMapper.update(null,
                new LambdaUpdateWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getConversationId, conversationId)
                        .eq(ChatConversationMember::getUserId, userId)
                        .set(ChatConversationMember::getLastReadMessageId, message.getId())
                        .set(ChatConversationMember::getLastReadTime, LocalDateTime.now()));

        // Build message VO
        Map<String, Object> messageVO = buildMessageVO(message);

        // Push to all conversation members via WebSocket
        List<ChatConversationMember> members = memberMapper.selectList(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getConversationId, conversationId));

        Map<String, Object> wsMsg = new LinkedHashMap<>();
        wsMsg.put("type", "chat_message");
        wsMsg.put("data", messageVO);

        for (ChatConversationMember m : members) {
            sessionManager.sendToUser(m.getUserId(), wsMsg);
            // Push updated notification counts for non-sender
            if (!m.getUserId().equals(userId)) {
                pushNotificationCount(m.getUserId());
            }
        }

        return messageVO;
    }

    @Override
    public void markConversationRead(String userId, String conversationId, String messageId) {
        if (messageId == null) {
            // Mark all as read - use the last message
            ChatConversation conv = conversationMapper.selectById(conversationId);
            if (conv != null && conv.getLastMessageId() != null) {
                messageId = conv.getLastMessageId();
            }
        }

        if (messageId != null) {
            memberMapper.update(null,
                    new LambdaUpdateWrapper<ChatConversationMember>()
                            .eq(ChatConversationMember::getConversationId, conversationId)
                            .eq(ChatConversationMember::getUserId, userId)
                            .set(ChatConversationMember::getLastReadMessageId, messageId)
                            .set(ChatConversationMember::getLastReadTime, LocalDateTime.now()));

            // Broadcast read receipt
            List<ChatConversationMember> members = memberMapper.selectList(
                    new LambdaQueryWrapper<ChatConversationMember>()
                            .eq(ChatConversationMember::getConversationId, conversationId));

            Map<String, Object> readReceipt = Map.of(
                    "type", "read_receipt",
                    "data", Map.of(
                            "conversationId", conversationId,
                            "userId", userId,
                            "messageId", messageId
                    )
            );
            for (ChatConversationMember m : members) {
                if (!m.getUserId().equals(userId)) {
                    sessionManager.sendToUser(m.getUserId(), readReceipt);
                }
            }

            // Update notification count
            pushNotificationCount(userId);
        }
    }

    @Override
    @Transactional
    public Map<String, Object> findOrCreatePrivateConversation(String userId, String targetUserId) {
        // Find existing private conversation between these two users
        List<ChatConversationMember> myMemberships = memberMapper.selectList(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getUserId, userId));

        for (ChatConversationMember myMember : myMemberships) {
            ChatConversation conv = conversationMapper.selectById(myMember.getConversationId());
            if (conv != null && "private".equals(conv.getType())) {
                ChatConversationMember otherMember = memberMapper.selectOne(
                        new LambdaQueryWrapper<ChatConversationMember>()
                                .eq(ChatConversationMember::getConversationId, conv.getId())
                                .eq(ChatConversationMember::getUserId, targetUserId));
                if (otherMember != null) {
                    return buildConversationVO(conv, myMember, userId);
                }
            }
        }

        // Create new private conversation
        return createConversation(userId, "private", "", List.of(targetUserId));
    }

    @Override
    public void broadcastTyping(String userId, String conversationId) {
        List<ChatConversationMember> members = memberMapper.selectList(
                new LambdaQueryWrapper<ChatConversationMember>()
                        .eq(ChatConversationMember::getConversationId, conversationId));

        SysUser sender = userMapper.selectById(userId);
        String senderName = sender != null
                ? (sender.getNickName() != null ? sender.getNickName() : sender.getUsername())
                : "Unknown";

        Map<String, Object> typingMsg = Map.of(
                "type", "typing",
                "data", Map.of(
                        "conversationId", conversationId,
                        "userId", userId,
                        "username", senderName
                )
        );

        for (ChatConversationMember m : members) {
            if (!m.getUserId().equals(userId)) {
                sessionManager.sendToUser(m.getUserId(), typingMsg);
            }
        }
    }

    private Map<String, Object> buildConversationVO(ChatConversation conv, ChatConversationMember member, String userId) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", conv.getId());
        map.put("type", conv.getType());
        map.put("name", conv.getName());
        map.put("avatar", conv.getAvatar() != null ? conv.getAvatar() : "");

        // For private chats, display the other user's info
        if ("private".equals(conv.getType())) {
            List<ChatConversationMember> members = memberMapper.selectList(
                    new LambdaQueryWrapper<ChatConversationMember>()
                            .eq(ChatConversationMember::getConversationId, conv.getId())
                            .ne(ChatConversationMember::getUserId, userId));
            if (!members.isEmpty()) {
                SysUser otherUser = userMapper.selectById(members.get(0).getUserId());
                if (otherUser != null) {
                    map.put("name", otherUser.getNickName() != null ? otherUser.getNickName() : otherUser.getUsername());
                    map.put("avatar", otherUser.getAvatar() != null ? otherUser.getAvatar() : "");
                    map.put("targetUserId", otherUser.getId());
                    map.put("online", sessionManager.isOnline(otherUser.getId()));
                }
            }
        }

        map.put("lastMessageTime", conv.getLastMessageTime() != null ? conv.getLastMessageTime().toString() : null);

        // Last message content
        if (conv.getLastMessageId() != null) {
            ChatMessage lastMsg = messageMapper.selectById(conv.getLastMessageId());
            map.put("lastMessage", lastMsg != null ? lastMsg.getContent() : "");
        } else {
            map.put("lastMessage", "");
        }

        // Unread count
        long unread = 0;
        if (member != null && conv.getLastMessageId() != null) {
            if (member.getLastReadMessageId() == null) {
                unread = messageMapper.selectCount(
                        new LambdaQueryWrapper<ChatMessage>()
                                .eq(ChatMessage::getConversationId, conv.getId()));
            } else {
                ChatMessage lastReadMsg = messageMapper.selectById(member.getLastReadMessageId());
                if (lastReadMsg != null) {
                    unread = messageMapper.selectCount(
                            new LambdaQueryWrapper<ChatMessage>()
                                    .eq(ChatMessage::getConversationId, conv.getId())
                                    .gt(ChatMessage::getCreatedAt, lastReadMsg.getCreatedAt()));
                }
            }
        }
        map.put("unread", unread);
        map.put("muted", member != null ? member.getMuted() : 0);
        map.put("createdAt", conv.getCreatedAt() != null ? conv.getCreatedAt().toString() : null);

        return map;
    }

    private Map<String, Object> buildMessageVO(ChatMessage msg) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", msg.getId());
        map.put("conversationId", msg.getConversationId());
        map.put("senderId", msg.getSenderId());
        map.put("content", msg.getContent());
        map.put("type", msg.getType());
        map.put("replyToId", msg.getReplyToId());
        map.put("createdAt", msg.getCreatedAt() != null ? msg.getCreatedAt().toString() : null);

        // Sender info
        SysUser sender = userMapper.selectById(msg.getSenderId());
        if (sender != null) {
            map.put("senderName", sender.getNickName() != null ? sender.getNickName() : sender.getUsername());
            map.put("senderAvatar", sender.getAvatar() != null ? sender.getAvatar() : "");
        } else {
            map.put("senderName", "Unknown");
            map.put("senderAvatar", "");
        }

        return map;
    }

    private void pushNotificationCount(String userId) {
        try {
            Map<String, Object> counts = notificationService.getCounts(userId);
            sessionManager.sendToUser(userId, Map.of("type", "notification_count", "data", counts));
        } catch (Exception e) {
            log.warn("Failed to push notification count: {}", e.getMessage());
        }
    }
}
