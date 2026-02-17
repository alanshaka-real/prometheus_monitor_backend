-- V19: 聊天与通知系统
-- 会话表
CREATE TABLE IF NOT EXISTS chat_conversation (
    id VARCHAR(36) PRIMARY KEY,
    type VARCHAR(10) NOT NULL COMMENT 'private/group',
    name VARCHAR(100) DEFAULT '',
    avatar VARCHAR(255) DEFAULT '',
    owner_id VARCHAR(36) DEFAULT NULL,
    last_message_id VARCHAR(36) DEFAULT NULL,
    last_message_time DATETIME DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_type (type),
    INDEX idx_last_message_time (last_message_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 会话成员表
CREATE TABLE IF NOT EXISTS chat_conversation_member (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    user_id VARCHAR(36) NOT NULL,
    role VARCHAR(20) DEFAULT 'member',
    last_read_message_id VARCHAR(36) DEFAULT NULL,
    last_read_time DATETIME DEFAULT NULL,
    muted TINYINT DEFAULT 0,
    joined_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_conversation_id (conversation_id),
    INDEX idx_user_id (user_id),
    UNIQUE KEY uk_conv_user (conversation_id, user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 聊天消息表
CREATE TABLE IF NOT EXISTS chat_message (
    id VARCHAR(36) PRIMARY KEY,
    conversation_id VARCHAR(36) NOT NULL,
    sender_id VARCHAR(36) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(20) DEFAULT 'text' COMMENT 'text/image/file/system',
    reply_to_id VARCHAR(36) DEFAULT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    deleted INT DEFAULT 0,
    INDEX idx_conv_created (conversation_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 系统通知表
CREATE TABLE IF NOT EXISTS sys_notification (
    id VARCHAR(36) PRIMARY KEY,
    user_id VARCHAR(36) NOT NULL,
    category VARCHAR(20) NOT NULL COMMENT 'notice/pending',
    type VARCHAR(30) NOT NULL COMMENT 'alert_firing/alert_resolved/deploy_success/deploy_failure/...',
    title VARCHAR(200) NOT NULL,
    content TEXT DEFAULT NULL,
    reference_id VARCHAR(36) DEFAULT NULL,
    reference_type VARCHAR(30) DEFAULT NULL,
    is_read TINYINT DEFAULT 0,
    is_handled TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    read_at DATETIME DEFAULT NULL,
    INDEX idx_user_category_read (user_id, category, is_read),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
