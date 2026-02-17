package com.wenmin.prometheus.module.distribute.service.impl;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 部署并发锁管理器。
 * 使用 ConcurrentHashMap 实现机器级别的互斥锁，防止同一台主机被并发部署。
 */
@Component
public class DeploymentLockManager {

    private final ConcurrentHashMap<String, String> locks = new ConcurrentHashMap<>();

    /**
     * 尝试获取指定机器的部署锁。
     *
     * @param machineKey 机器标识（machineId 或 IP）
     * @param taskId     当前任务 ID
     * @return true 表示成功获取锁，false 表示该机器已被其他任务锁定
     */
    public boolean tryLock(String machineKey, String taskId) {
        return locks.putIfAbsent(machineKey, taskId) == null;
    }

    /**
     * 释放指定机器的部署锁。
     * 仅当锁持有者与传入的 taskId 匹配时才释放，防止误释放其他任务的锁。
     *
     * @param machineKey 机器标识
     * @param taskId     当前任务 ID
     */
    public void unlock(String machineKey, String taskId) {
        locks.remove(machineKey, taskId);
    }

    /**
     * 获取当前持有锁的任务 ID。
     *
     * @param machineKey 机器标识
     * @return 持有锁的任务 ID，若未锁定则返回 null
     */
    public String getHolder(String machineKey) {
        return locks.get(machineKey);
    }
}
