package com.junsong.system.mapper;

/**
 * 数据质量只读统计 mapper。
 * R20: 所有方法只读，不写数据库。
 */
public interface SysDataQualityMapper {

    /** 销售记录缺少门店/部门 */
    Long countFinanceSaleWithoutDept();

    /** 缴款记录缺销售单 */
    Long countFinancePaymentWithoutSale();

    /** 逾期应收缺负责人/跟进人 */
    Long countFinanceReceivableOverdueWithoutOwner();

    /** 会员缺可识别联系方式 */
    Long countMemberWithoutPhoneAndOpenid();

    /** 已执行增长动作缺效果统计 */
    Long countMemberGrowthActionWithoutEffect();

    /** 库存当前结存为负 */
    Long countNegativeStockPosition();

    /** 菜单组件为空 */
    Long countSystemMenuComponentEmpty();
}
