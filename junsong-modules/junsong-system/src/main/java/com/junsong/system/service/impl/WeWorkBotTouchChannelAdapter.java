package com.junsong.system.service.impl;

import java.util.LinkedHashMap;
import java.util.Map;

import com.junsong.system.domain.vo.ActionCenterItemVO;
import com.junsong.system.domain.vo.ActionTouchRequestVO;
import com.junsong.system.domain.vo.ActionTouchResultVO;
import com.junsong.system.service.ISysConfigService;
import com.junsong.system.service.ITouchChannelAdapter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * R22 企业微信群机器人触达通道适配器。
 * 通道开关、dryRun、webhookUrl 均来自 sys_config，DEV 默认 dry-run 不真实发送。
 */
@Component
public class WeWorkBotTouchChannelAdapter implements ITouchChannelAdapter {

    private static final String CONFIG_ENABLED = "r22.touch.wework.enabled";
    private static final String CONFIG_DRY_RUN = "r22.touch.wework.dryRun";
    private static final String CONFIG_WEBHOOK_URL = "r22.touch.wework.webhookUrl";

    private final ISysConfigService configService;
    private final RestTemplate restTemplate = new RestTemplate();

    public WeWorkBotTouchChannelAdapter(ISysConfigService configService) {
        this.configService = configService;
    }

    @Override
    public String channel() {
        return "WEWORK_BOT";
    }

    @Override
    public ActionTouchResultVO send(ActionCenterItemVO action, ActionTouchRequestVO request) {
        ActionTouchResultVO result = new ActionTouchResultVO();
        result.setActionId(action.getActionId());
        result.setChannel(channel());

        if (!isEnabled()) {
            result.setTouchStatus("DISABLED");
            result.setMessage("企业微信群机器人通道未启用");
            return result;
        }

        if (isDryRun()) {
            result.setTouchStatus("DRY_RUN");
            result.setMessage("dryRun 模式：未真实发送，消息已生成");
            result.setProviderResponse(buildMarkdown(action));
            return result;
        }

        String webhookUrl = readWebhookUrl();
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            result.setTouchStatus("FAILED");
            result.setMessage("webhook URL 未配置");
            return result;
        }

        try {
            String markdown = buildMarkdown(action);
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("msgtype", "markdown");
            Map<String, Object> content = new LinkedHashMap<>();
            content.put("content", markdown);
            payload.put("markdown", content);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(payload, headers);

            ResponseEntity<String> resp = restTemplate.postForEntity(webhookUrl, entity, String.class);
            String body = resp.getBody();
            result.setTouchStatus("SUCCESS");
            result.setMessage("已发送至企业微信群机器人");
            result.setProviderResponse(truncate(body, 2000));
        } catch (Exception e) {
            result.setTouchStatus("FAILED");
            result.setMessage("调用企业微信群机器人失败：" + safeMessage(e));
        }
        return result;
    }

    private String buildMarkdown(ActionCenterItemVO action) {
        StringBuilder sb = new StringBuilder();
        sb.append("【君颂动作中心】\n");
        sb.append("优先级：").append(nullSafe(action.getPriority())).append("\n");
        sb.append("来源：").append(nullSafe(action.getSourceType())).append("\n");
        sb.append("标题：").append(nullSafe(action.getTitle())).append("\n");
        sb.append("门店：").append(nullSafe(action.getDeptName())).append("\n");
        sb.append("处理人：").append(nullSafe(action.getOwnerName())).append("\n");
        sb.append("入口：").append(nullSafe(action.getDrilldownPath()));
        return sb.toString();
    }

    private boolean isEnabled() {
        return "true".equalsIgnoreCase(readConfig(CONFIG_ENABLED));
    }

    private boolean isDryRun() {
        return "true".equalsIgnoreCase(readConfig(CONFIG_DRY_RUN));
    }

    private String readWebhookUrl() {
        return readConfig(CONFIG_WEBHOOK_URL);
    }

    private String readConfig(String key) {
        try {
            return configService.selectConfigByKey(key);
        } catch (Exception e) {
            return null;
        }
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }

    private String safeMessage(Throwable e) {
        return e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
    }
}
