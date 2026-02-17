package com.wenmin.prometheus.module.alert.scheduler;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 告警收敛 / 降噪管理器。
 * <p>
 * 按 {@code groupName + severity} 对告警进行分组，并实现三级时间窗控制：
 * <ul>
 *   <li><b>group_wait</b> — 一个新分组首次产生告警后，等待该时长再发送第一条通知，
 *       以便将短时间内同组的多条告警聚合为一条通知，默认 30 秒。</li>
 *   <li><b>group_interval</b> — 同一分组内告警指纹集合发生变化（新增或恢复）时，
 *       距上次通知至少等待该时长后再发送更新通知，默认 5 分钟。</li>
 *   <li><b>repeat_interval</b> — 同一分组内告警指纹集合未发生变化时，
 *       距上次通知至少等待该时长后才重复发送，默认 4 小时。</li>
 * </ul>
 * <p>
 * 线程安全：所有可变状态均存储在 {@link ConcurrentHashMap} 中，
 * 单个分组的状态变更通过 {@code synchronized} 保护。
 *
 * @see AlertEvaluationScheduler
 */
@Slf4j
@Component
public class AlertGroupManager {

    // --------------- 可配置参数 ---------------

    /** 新分组首次告警后等待时长（毫秒），默认 30 秒 */
    @Value("${alert.convergence.group-wait-ms:30000}")
    private long groupWaitMs;

    /** 分组内告警有变化时，最短通知间隔（毫秒），默认 5 分钟 */
    @Value("${alert.convergence.group-interval-ms:300000}")
    private long groupIntervalMs;

    /** 分组内告警无变化时，重复通知间隔（毫秒），默认 4 小时 */
    @Value("${alert.convergence.repeat-interval-ms:14400000}")
    private long repeatIntervalMs;

    // --------------- 分组状态 ---------------

    /**
     * 分组状态映射表。key = groupKey（groupName:severity）
     */
    private final ConcurrentHashMap<String, AlertGroupState> groupStates = new ConcurrentHashMap<>();

    // --------------- 内部状态类 ---------------

    /**
     * 单个告警分组的运行时状态。
     * <p>
     * 所有字段的读写必须在持有该对象监视器锁（synchronized）的条件下执行。
     */
    private static class AlertGroupState {
        /** 分组首次出现的时间戳（毫秒） */
        long firstSeenAt;
        /** 上次成功发送通知的时间戳（毫秒），0 表示尚未发送过 */
        long lastNotifiedAt;
        /** 上次通知时的告警指纹快照（不可变），用于判断分组内容是否变化 */
        Set<String> lastNotifiedFingerprints;
        /** 待发送的告警指纹累积集合，在等待窗口期间不断收集 */
        final Set<String> pendingFingerprints;

        AlertGroupState(long now) {
            this.firstSeenAt = now;
            this.lastNotifiedAt = 0;
            this.lastNotifiedFingerprints = Collections.emptySet();
            this.pendingFingerprints = new HashSet<>();
        }
    }

    // --------------- 公开 API ---------------

    /**
     * 判断指定告警是否应当触发通知。
     * <p>
     * 调用方在每次告警评估时调用此方法；返回 {@code true} 表示应当发送通知，
     * 同时内部会自动更新分组状态（lastNotifiedAt 与 fingerprints）。
     *
     * @param alertName 告警名称，用于生成告警指纹
     * @param severity  严重级别（critical / warning / info）
     * @param groupName 分组名称
     * @return true 表示应当发送通知
     */
    public boolean shouldNotify(String alertName, String severity, String groupName) {
        String groupKey = buildGroupKey(groupName, severity);
        String fingerprint = buildFingerprint(alertName, severity);
        long now = Instant.now().toEpochMilli();

        // computeIfAbsent 保证只创建一次 state 对象
        AlertGroupState state = groupStates.computeIfAbsent(groupKey, key -> {
            log.debug("告警分组首次出现: groupKey={}, alertName={}", key, alertName);
            return new AlertGroupState(now);
        });

        // 对单个分组的所有状态变更加锁，保证一致性
        synchronized (state) {
            // 将当前告警指纹加入待发送集合
            state.pendingFingerprints.add(fingerprint);

            // ---------- 阶段 1: group_wait ----------
            // 该分组从未发送过通知，需要等待 groupWaitMs 再发首次通知
            if (state.lastNotifiedAt == 0) {
                long elapsed = now - state.firstSeenAt;
                if (elapsed < groupWaitMs) {
                    log.debug("告警分组等待中 (group_wait): groupKey={}, elapsed={}ms, wait={}ms",
                            groupKey, elapsed, groupWaitMs);
                    return false;
                }
                // group_wait 已过 — 发送首次通知并快照当前指纹
                log.info("告警分组首次通知 (group_wait 已满足): groupKey={}", groupKey);
                state.lastNotifiedAt = now;
                state.lastNotifiedFingerprints = Set.copyOf(state.pendingFingerprints);
                return true;
            }

            // ---------- 阶段 2 / 3: 已发送过通知 ----------
            // 判断指纹集合是否相对上次通知有变化
            boolean fingerprintsChanged = !state.lastNotifiedFingerprints.containsAll(state.pendingFingerprints);
            long sinceLastNotify = now - state.lastNotifiedAt;

            if (fingerprintsChanged) {
                // 有新告警加入 — 适用 group_interval
                if (sinceLastNotify < groupIntervalMs) {
                    log.debug("告警分组更新等待中 (group_interval): groupKey={}, sinceLastNotify={}ms, interval={}ms",
                            groupKey, sinceLastNotify, groupIntervalMs);
                    return false;
                }
                log.info("告警分组更新通知 (group_interval 已满足): groupKey={}, newAlert={}",
                        groupKey, alertName);
                state.lastNotifiedAt = now;
                state.lastNotifiedFingerprints = Set.copyOf(state.pendingFingerprints);
                return true;
            }

            // 指纹集合未变化 — 适用 repeat_interval
            if (sinceLastNotify < repeatIntervalMs) {
                log.debug("告警分组重复等待中 (repeat_interval): groupKey={}, sinceLastNotify={}ms, interval={}ms",
                        groupKey, sinceLastNotify, repeatIntervalMs);
                return false;
            }

            log.info("告警分组重复通知 (repeat_interval 已满足): groupKey={}", groupKey);
            state.lastNotifiedAt = now;
            state.lastNotifiedFingerprints = Set.copyOf(state.pendingFingerprints);
            return true;
        }
    }

    /**
     * 从分组的待发送指纹集合中移除指定告警（例如该告警已恢复）。
     * <p>
     * 若移除后分组内无剩余告警，则自动清除该分组状态。
     *
     * @param alertName 告警名称
     * @param severity  严重级别
     * @param groupName 分组名称
     */
    public void removeAlert(String alertName, String severity, String groupName) {
        String groupKey = buildGroupKey(groupName, severity);
        String fingerprint = buildFingerprint(alertName, severity);

        AlertGroupState state = groupStates.get(groupKey);
        if (state == null) {
            return;
        }

        synchronized (state) {
            state.pendingFingerprints.remove(fingerprint);
            if (state.pendingFingerprints.isEmpty()) {
                groupStates.remove(groupKey);
                log.info("告警分组已空，状态已清除: groupKey={}", groupKey);
            }
        }
    }

    /**
     * 清除指定分组的全部状态（例如分组内所有告警均已恢复时调用）。
     *
     * @param groupName 分组名称
     * @param severity  严重级别
     */
    public void clearGroup(String groupName, String severity) {
        String groupKey = buildGroupKey(groupName, severity);
        AlertGroupState removed = groupStates.remove(groupKey);
        if (removed != null) {
            log.info("告警分组状态已清除: groupKey={}", groupKey);
        }
    }

    /**
     * 清除所有分组状态（用于测试或重置场景）。
     */
    public void clearAll() {
        groupStates.clear();
        log.info("所有告警分组状态已清除");
    }

    /**
     * 获取当前正在跟踪的分组数量（用于监控 / 调试）。
     *
     * @return 分组数量
     */
    public int getActiveGroupCount() {
        return groupStates.size();
    }

    /**
     * 判断指定分组是否存在且正在被跟踪。
     *
     * @param groupName 分组名称
     * @param severity  严重级别
     * @return true 表示分组正在被跟踪
     */
    public boolean isGroupTracked(String groupName, String severity) {
        return groupStates.containsKey(buildGroupKey(groupName, severity));
    }

    // --------------- 内部方法 ---------------

    /**
     * 构建分组 key：{groupName}:{severity}。
     * 对 null / 空值做安全处理，使用默认值兜底。
     */
    private String buildGroupKey(String groupName, String severity) {
        String gn = (groupName == null || groupName.isBlank()) ? "default" : groupName.trim();
        String sv = (severity == null || severity.isBlank()) ? "warning" : severity.trim();
        return gn + ":" + sv;
    }

    /**
     * 构建告警指纹：基于 alertName 和 severity 生成确定性字符串。
     * 同一条告警规则在同一严重级别下始终产生相同的指纹。
     */
    private String buildFingerprint(String alertName, String severity) {
        String name = (alertName == null) ? "" : alertName;
        String sev = (severity == null) ? "" : severity;
        return Objects.hash(name, sev) + ":" + name;
    }
}
