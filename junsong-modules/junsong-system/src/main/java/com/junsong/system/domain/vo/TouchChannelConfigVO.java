package com.junsong.system.domain.vo;

public class TouchChannelConfigVO {
    private Boolean enabled;
    private Boolean dryRun;
    private String webhookUrl;
    private int rateLimitPerTarget24h;

    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public Boolean getDryRun() { return dryRun; }
    public void setDryRun(Boolean dryRun) { this.dryRun = dryRun; }
    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
    public int getRateLimitPerTarget24h() { return rateLimitPerTarget24h; }
    public void setRateLimitPerTarget24h(int rateLimitPerTarget24h) { this.rateLimitPerTarget24h = rateLimitPerTarget24h; }
}
