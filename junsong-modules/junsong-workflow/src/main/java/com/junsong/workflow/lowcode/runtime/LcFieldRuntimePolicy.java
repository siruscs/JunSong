package com.junsong.workflow.lowcode.runtime;

import com.junsong.workflow.lowcode.domain.LcBizField;

/**
 * 将字段元数据转换为页面运行时能力，统一决定字段是否显示、是否可编辑、是否必填。
 * 页面只消费结果，不自行猜测字段语义。
 */
public final class LcFieldRuntimePolicy
{
    private LcFieldRuntimePolicy() { }

    public static Policy resolve(LcBizField field, String pageType)
    {
        if (field == null || pageType == null || pageType.isBlank())
        {
            return new Policy(false, false, false);
        }
        String page = pageType.toUpperCase();
        boolean visible = switch (page)
        {
            case "LIST" -> enabled(field.getIsList());
            case "DETAIL", "VIEW" -> enabled(field.getIsDetail());
            case "FORM", "CREATE", "EDIT", "PROCESS", "FULFILLMENT" -> appliesToStage(field, page);
            default -> false;
        };
        boolean editable = visible && switch (page)
        {
            case "DETAIL", "VIEW" -> false;
            case "PROCESS", "FULFILLMENT" -> "FULFILLMENT".equalsIgnoreCase(field.getStage())
                    || "PROCESS".equalsIgnoreCase(field.getStage())
                    || isBlank(field.getStage());
            default -> true;
        };
        boolean required = editable && enabled(field.getRequired());
        return new Policy(visible, editable, required);
    }

    private static boolean appliesToStage(LcBizField field, String page)
    {
        return isBlank(field.getStage()) || page.equalsIgnoreCase(field.getStage());
    }

    private static boolean enabled(String value) { return "1".equals(value) || "Y".equalsIgnoreCase(value); }
    private static boolean isBlank(String value) { return value == null || value.isBlank(); }

    public record Policy(boolean visible, boolean editable, boolean required) { }
}
