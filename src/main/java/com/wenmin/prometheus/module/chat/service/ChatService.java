package com.wenmin.prometheus.module.chat.service;

import java.util.List;
import java.util.Map;

public interface ChatService {

    List<Map<String, Object>> getContacts(String userId, String keyword);

    List<Map<String, Object>> getConversations(String userId);

    Map<String, Object> createConversation(String userId, String type, String name, List<String> memberIds);

    Map<String, Object> getMessages(String conversationId, String userId, Integer page, Integer pageSize);

    Map<String, Object> sendMessage(String userId, String conversationId, String content, String type);

    void markConversationRead(String userId, String conversationId, String messageId);

    Map<String, Object> findOrCreatePrivateConversation(String userId, String targetUserId);

    // WebSocket methods
    void sendMessageViaWebSocket(String userId, String conversationId, String content);

    void broadcastTyping(String userId, String conversationId);
}
