package com.junsong.system.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.system.domain.SysActionCenterTouchLog;
import com.junsong.system.domain.SysActionCenterTouchThrottle;
import com.junsong.system.domain.vo.ActionCenterItemVO;
import com.junsong.system.domain.vo.ActionTouchRequestVO;
import com.junsong.system.domain.vo.ActionTouchResultVO;
import com.junsong.system.mapper.SysActionCenterTouchLogMapper;
import com.junsong.system.mapper.SysActionCenterTouchThrottleMapper;
import com.junsong.system.service.ISysActionCenterService;
import com.junsong.system.service.ISysConfigService;
import com.junsong.system.service.ITouchChannelAdapter;
import org.springframework.stereotype.Service;

/**
 * R22 动作触达服务：负责去重（duplicate）、限流（rateLimit）、通道派发、日志落库与节流更新。
 */
@Service
public class SysActionTouchServiceImpl implements com.junsong.system.service.ISysActionTouchService {

    private static final String DEFAULT_CHANNEL = "WEWORK_BOT";
    private static final String DEFAULT_TARGET_TYPE = "GROUP";
    private static final String CONFIG_RATE_LIMIT = "r22.touch.rateLimit.perTarget24h";
    private static final int DEFAULT_RATE_LIMIT = 3;

    private final ISysActionCenterService actionCenterService;
    private final List<ITouchChannelAdapter> adapters;
    private final SysActionCenterTouchLogMapper touchLogMapper;
    private final SysActionCenterTouchThrottleMapper throttleMapper;
    private final ISysConfigService configService;

    public SysActionTouchServiceImpl(ISysActionCenterService actionCenterService,
                                     List<ITouchChannelAdapter> adapters,
                                     SysActionCenterTouchLogMapper touchLogMapper,
                                     SysActionCenterTouchThrottleMapper throttleMapper,
                                     ISysConfigService configService) {
        this.actionCenterService = actionCenterService;
        this.adapters = adapters;
        this.touchLogMapper = touchLogMapper;
        this.throttleMapper = throttleMapper;
        this.configService = configService;
    }

    @Override
    public ActionTouchResultVO touch(String actionId, ActionTouchRequestVO request) {
        ActionCenterItemVO action = actionCenterService.getAction(actionId);
        if (action == null) {
            return failed(actionId, DEFAULT_CHANNEL, "动作不存在：" + actionId);
        }
        if (!Boolean.TRUE.equals(action.getTouchable())) {
            return failed(actionId, DEFAULT_CHANNEL,
                    "动作不可触达：" + (action.getTouchDisabledReason() != null ? action.getTouchDisabledReason() : "当前状态不允许触达"));
        }
        String channel = request.getChannel() != null ? request.getChannel() : DEFAULT_CHANNEL;
        String targetType = request.getTargetType() != null ? request.getTargetType() : DEFAULT_TARGET_TYPE;
        String targetRef = request.getTargetRef() != null ? request.getTargetRef() : "";
        boolean force = Boolean.TRUE.equals(request.getForce());
        String messageSummary = buildSummary(action, request);

        String digest = digest(channel + "|" + actionId + "|" + targetType + "|" + targetRef + "|" + messageSummary);

        if (!force) {
            Date since30min = new Date(System.currentTimeMillis() - TimeUnit.MINUTES.toMillis(30));
            if (touchLogMapper.countByDigestSince(digest, since30min) > 0) {
                return logAndReturn(action, channel, targetType, targetRef, digest, messageSummary,
                        "SKIPPED_DUPLICATE", "近30分钟内已存在相同触达请求，已跳过", null);
            }
            int rateLimit = readRateLimit();
            Date since24h = new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24));
            if (touchLogMapper.countByTargetSince(channel, targetRef, since24h) >= rateLimit) {
                return logAndReturn(action, channel, targetType, targetRef, digest, messageSummary,
                        "SKIPPED_RATE_LIMIT", "目标近24小时触达已达上限(" + rateLimit + "次)，已跳过", null);
            }
        }

        ITouchChannelAdapter adapter = selectAdapter(channel);
        if (adapter == null) {
            return logAndReturn(action, channel, targetType, targetRef, digest, messageSummary,
                    "FAILED", "未找到触达通道：" + channel, null);
        }

        ActionTouchRequestVO normalized = new ActionTouchRequestVO();
        normalized.setChannel(channel);
        normalized.setTargetType(targetType);
        normalized.setTargetRef(targetRef);
        normalized.setMessage(request.getMessage());
        normalized.setForce(force);

        ActionTouchResultVO result = adapter.send(action, normalized);
        if (result == null) {
            result = failed(actionId, channel, "通道返回空结果");
        }
        result.setActionId(actionId);
        result.setChannel(channel);
        ActionTouchResultVO logged = logAndReturn(action, channel, targetType, targetRef, digest, messageSummary,
                result.getTouchStatus(), result.getMessage(), result.getProviderResponse());
        logged.setLogId(logged.getLogId());
        upsertThrottle(channel, targetRef, action.getSourceType());
        return logged;
    }

    private ActionTouchResultVO logAndReturn(ActionCenterItemVO action, String channel, String targetType,
                                             String targetRef, String digest, String messageSummary,
                                             String touchStatus, String message, String providerResponse) {
        SysActionCenterTouchLog log = new SysActionCenterTouchLog();
        log.setActionId(action.getActionId());
        log.setSourceType(action.getSourceType());
        log.setSourceId(action.getSourceId() != null ? action.getSourceId() : action.getActionId());
        log.setChannel(channel);
        log.setTargetType(targetType);
        log.setTargetRef(targetRef);
        log.setTouchStatus(touchStatus);
        log.setRequestDigest(digest);
        log.setMessageSummary(messageSummary);
        log.setProviderResponse(providerResponse);
        if ("FAILED".equals(touchStatus)) {
            log.setErrorMessage(message);
        }
        try {
            log.setOperatorId(SecurityUtils.getUserId());
            log.setOperatorName(SecurityUtils.getUsername());
        } catch (Exception ignore) {
        }
        touchLogMapper.insertLog(log);
        ActionTouchResultVO vo = new ActionTouchResultVO();
        vo.setLogId(log.getLogId());
        vo.setActionId(action.getActionId());
        vo.setChannel(channel);
        vo.setTouchStatus(touchStatus);
        vo.setMessage(message);
        vo.setProviderResponse(providerResponse);
        return vo;
    }

    private void upsertThrottle(String channel, String targetRef, String sourceType) {
        try {
            String throttleKey = channel + ":" + targetRef + ":" + (sourceType != null ? sourceType : "");
            SysActionCenterTouchThrottle existing = throttleMapper.selectByKey(throttleKey);
            Date now = new Date();
            if (existing == null) {
                SysActionCenterTouchThrottle throttle = new SysActionCenterTouchThrottle();
                throttle.setThrottleKey(throttleKey);
                throttle.setChannel(channel);
                throttle.setTargetRef(targetRef);
                throttle.setLastTouchTime(now);
                throttle.setTouchCount24h(1);
                throttleMapper.insertThrottle(throttle);
            } else {
                Date since24h = new Date(System.currentTimeMillis() - TimeUnit.HOURS.toMillis(24));
                int count = (existing.getLastTouchTime() != null && !existing.getLastTouchTime().before(since24h))
                        ? existing.getTouchCount24h() + 1 : 1;
                existing.setLastTouchTime(now);
                existing.setTouchCount24h(count);
                throttleMapper.updateThrottle(existing);
            }
        } catch (Exception ignore) {
        }
    }

    private ITouchChannelAdapter selectAdapter(String channel) {
        if (adapters == null) {
            return null;
        }
        for (ITouchChannelAdapter adapter : adapters) {
            if (channel.equalsIgnoreCase(adapter.channel())) {
                return adapter;
            }
        }
        return null;
    }

    private int readRateLimit() {
        try {
            String value = configService.selectConfigByKey(CONFIG_RATE_LIMIT);
            if (value != null && !value.isEmpty()) {
                return Integer.parseInt(value.trim());
            }
        } catch (Exception ignore) {
        }
        return DEFAULT_RATE_LIMIT;
    }

    private String buildSummary(ActionCenterItemVO action, ActionTouchRequestVO request) {
        StringBuilder sb = new StringBuilder();
        if (action.getTitle() != null) {
            sb.append(action.getTitle());
        }
        if (action.getSourceType() != null) {
            sb.append("|").append(action.getSourceType());
        }
        if (request.getMessage() != null && !request.getMessage().isEmpty()) {
            sb.append("|").append(request.getMessage());
        }
        return sb.toString();
    }

    private String digest(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return Integer.toHexString(input.hashCode());
        }
    }

    private ActionTouchResultVO failed(String actionId, String channel, String message) {
        ActionTouchResultVO vo = new ActionTouchResultVO();
        vo.setActionId(actionId);
        vo.setChannel(channel);
        vo.setTouchStatus("FAILED");
        vo.setMessage(message);
        return vo;
    }
}
