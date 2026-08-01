package com.junsong.finance.domain.vo;

import java.math.BigDecimal;

/**
 * 当日库存流水量视图：聚合某门店某商品某日的入库/出库数量。
 *
 * - inQuantity：当日正向流水（change_quantity &gt; 0）合计
 * - outQuantity：当日反向流水（change_quantity &lt; 0）绝对值合计
 *
 * @author junsong
 */
public class DailyFlowView {

    private BigDecimal inQuantity;
    private BigDecimal outQuantity;

    public BigDecimal getInQuantity() { return inQuantity; }
    public void setInQuantity(BigDecimal inQuantity) { this.inQuantity = inQuantity; }

    public BigDecimal getOutQuantity() { return outQuantity; }
    public void setOutQuantity(BigDecimal outQuantity) { this.outQuantity = outQuantity; }
}
