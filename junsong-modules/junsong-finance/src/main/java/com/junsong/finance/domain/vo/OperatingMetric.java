package com.junsong.finance.domain.vo;

import java.math.BigDecimal;
import java.util.List;

/**
 * 统一经营指标响应契约（Phase 5）。
 *
 * PC 和小程序通过同一端点获取指标，保证口径一致。
 * 旧 dashboard 接口保留兼容，客户端逐步迁移。
 */
public class OperatingMetric {

    /** 指标码（如 sales / expense / netCashflow） */
    private String code;
    /** 数值 */
    private BigDecimal value;
    /** 单位：CNY / COUNT / PERCENT */
    private String unit;
    /** 时间范围 */
    private Period period;
    /** 门店与租户范围 */
    private Scope scope;
    /** 数据来源 */
    private Source source;
    /** PC 跳转路由；小程序由 notificationTarget.js 映射 */
    private String drillDownRoute;

    // ── 内嵌类型 ──

    public static class Period {
        /** TODAY / MONTH / CURRENT_PERIOD / CUSTOM */
        private String type;
        private String start;
        private String end;

        public Period() {}
        public Period(String type, String start, String end) {
            this.type = type; this.start = start; this.end = end;
        }
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        public String getStart() { return start; }
        public void setStart(String start) { this.start = start; }
        public String getEnd() { return end; }
        public void setEnd(String end) { this.end = end; }
    }

    public static class Scope {
        private List<Long> deptIds;
        private String tenantId;

        public Scope() {}
        public Scope(List<Long> deptIds, String tenantId) {
            this.deptIds = deptIds; this.tenantId = tenantId;
        }
        public List<Long> getDeptIds() { return deptIds; }
        public void setDeptIds(List<Long> deptIds) { this.deptIds = deptIds; }
        public String getTenantId() { return tenantId; }
        public void setTenantId(String tenantId) { this.tenantId = tenantId; }
    }

    public static class Source {
        /** FINANCE / MEMBER / STOCK / SYSTEM */
        private String module;
        /** 后端端点 */
        private String endpoint;

        public Source() {}
        public Source(String module, String endpoint) {
            this.module = module; this.endpoint = endpoint;
        }
        public String getModule() { return module; }
        public void setModule(String module) { this.module = module; }
        public String getEndpoint() { return endpoint; }
        public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    }

    // ── getters / setters ──

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public BigDecimal getValue() { return value; }
    public void setValue(BigDecimal value) { this.value = value; }
    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }
    public Period getPeriod() { return period; }
    public void setPeriod(Period period) { this.period = period; }
    public Scope getScope() { return scope; }
    public void setScope(Scope scope) { this.scope = scope; }
    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }
    public String getDrillDownRoute() { return drillDownRoute; }
    public void setDrillDownRoute(String drillDownRoute) { this.drillDownRoute = drillDownRoute; }
}
