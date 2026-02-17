package com.wenmin.prometheus.module.chat.controller;

import com.wenmin.prometheus.common.result.R;
import com.wenmin.prometheus.module.chat.service.ChatService;
import com.wenmin.prometheus.security.SecurityUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "聊天管理")
@RestController
@RequestMapping("/api/prometheus/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @Operation(summary = "可聊天用户列表")
    @GetMapping("/contacts")
    public R<List<Map<String, Object>>> getContacts(
            @AuthenticationPrincipal SecurityUser user,
            @RequestParam(required = false) String keyword) {
        return R.ok(chatService.getContacts(user.getUserId(), keyword));
    }

    @Operation(summary = "会话列表")
    @GetMapping("/conversations")
    public R<List<Map<String, Object>>> getConversations(@AuthenticationPrincipal SecurityUser user) {
        return R.ok(chatService.getConversations(user.getUserId()));
    }

    @Operation(summary = "创建会话")
    @PostMapping("/conversations")
    @SuppressWarnings("unchecked")
    public R<Map<String, Object>> createConversation(
            @AuthenticationPrincipal SecurityUser user,
            @RequestBody Map<String, Object> body) {
        String type = (String) body.getOrDefault("type", "private");
        String name = (String) body.getOrDefault("name", "");
        List<String> memberIds = (List<String>) body.get("memberIds");
        return R.ok(chatService.createConversation(user.getUserId(), type, name, memberIds));
    }

    @Operation(summary = "消息历史")
    @GetMapping("/conversations/{id}/messages")
    public R<Map<String, Object>> getMessages(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable String id,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "30") Integer pageSize) {
        return R.ok(chatService.getMessages(id, user.getUserId(), page, pageSize));
    }

    @Operation(summary = "发送消息")
    @PostMapping("/conversations/{id}/messages")
    public R<Map<String, Object>> sendMessage(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        String content = body.get("content");
        String type = body.getOrDefault("type", "text");
        return R.ok(chatService.sendMessage(user.getUserId(), id, content, type));
    }

    @Operation(summary = "标记会话已读")
    @PutMapping("/conversations/{id}/read")
    public R<Void> markRead(
            @AuthenticationPrincipal SecurityUser user,
            @PathVariable String id) {
        chatService.markConversationRead(user.getUserId(), id, null);
        return R.ok();
    }

    @Operation(summary = "查找/创建私聊")
    @GetMapping("/conversations/find-private")
    public R<Map<String, Object>> findPrivate(
            @AuthenticationPrincipal SecurityUser user,
            @RequestParam String targetUserId) {
        return R.ok(chatService.findOrCreatePrivateConversation(user.getUserId(), targetUserId));
    }
}
