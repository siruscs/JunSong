package com.junsong.finance.domain.vo;

/**
 * 当日库存流水量视图：聚合某门店某商品某日的入库/出库数量。
 *
 * - inQuantity：当日正向流水（change_quantity &gt; 0）合计
 * - outQuantity：当日反向流水（change_quantity &lt; 0）绝对值合计
 *
 * @author junsong
 */
public class DailyFlowView {

    private Integer inQuantity;
    private Integer outQuantity;

    public Integer getInQuantity() { return inQuantity; }
    public void setInQuantity(Integer inQuantity) { this.inQuantity = inQuantity; }

    public Integer getOutQuantity() { return outQuantity; }
    public void setOutQuantity(Integer outQuantity) { this.outQuantity = outQuantity; }
}
