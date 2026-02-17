package com.wenmin.prometheus.security;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于内存的登录限流器，使用滑动窗口算法防止暴力破解。
 * <p>
 * 规则：同一 IP 在 5 分钟窗口内最多允许 5 次登录尝试，超过后锁定 15 分钟。
 */
@Component
public class LoginRateLimiter {

    /** key: IP 地址, value: 请求时间戳列表 */
    private final ConcurrentHashMap<String, List<Long>> requestMap = new ConcurrentHashMap<>();

    /** key: IP 地址, value: 锁定到期时间戳 */
    private final ConcurrentHashMap<String, Long> lockMap = new ConcurrentHashMap<>();

    private static final int MAX_ATTEMPTS = 5;
    private static final long WINDOW_MS = 300_000L;       // 5 分钟窗口
    private static final long LOCK_DURATION_MS = 900_000L; // 锁定 15 分钟

    /**
     * 检查是否允许通过。
     *
     * @param key 限流维度（通常为客户端 IP）
     * @return true 表示允许通过，false 表示被限流
     */
    public boolean tryAcquire(String key) {
        long now = System.currentTimeMillis();

        // 检查是否处于锁定期
        Long lockUntil = lockMap.get(key);
        if (lockUntil != null) {
            if (now < lockUntil) {
                return false;
            }
            // 锁定已过期，清除锁定记录
            lockMap.remove(key);
        }

        // 获取或创建请求时间戳列表
        List<Long> timestamps = requestMap.computeIfAbsent(key, k -> new ArrayList<>());

        synchronized (timestamps) {
            // 清除窗口外的旧记录
            long windowStart = now - WINDOW_MS;
            timestamps.removeIf(ts -> ts < windowStart);

            // 判断窗口内请求数是否超限
            if (timestamps.size() >= MAX_ATTEMPTS) {
                // 触发锁定
                lockMap.put(key, now + LOCK_DURATION_MS);
                timestamps.clear();
                return false;
            }

            // 记录本次请求
            timestamps.add(now);
            return true;
        }
    }

    /**
     * 定时清理过期记录，每 10 分钟执行一次。
     */
    @Scheduled(fixedRate = 600_000)
    public void cleanup() {
        long now = System.currentTimeMillis();

        // 清理过期锁定
        Iterator<Map.Entry<String, Long>> lockIterator = lockMap.entrySet().iterator();
        while (lockIterator.hasNext()) {
            if (now >= lockIterator.next().getValue()) {
                lockIterator.remove();
            }
        }

        // 清理过期的滑动窗口记录
        long windowStart = now - WINDOW_MS;
        Iterator<Map.Entry<String, List<Long>>> reqIterator = requestMap.entrySet().iterator();
        while (reqIterator.hasNext()) {
            Map.Entry<String, List<Long>> entry = reqIterator.next();
            List<Long> timestamps = entry.getValue();
            synchronized (timestamps) {
                timestamps.removeIf(ts -> ts < windowStart);
                if (timestamps.isEmpty()) {
                    reqIterator.remove();
                }
            }
        }
    }
}
