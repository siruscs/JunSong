package com.junsong.finance.domain.vo;

import java.util.List;
import com.junsong.finance.domain.FinStockInitBatch;
import com.junsong.finance.domain.FinStockInitItem;

/**
 * 期初库存批次详情视图。
 *
 * @author junsong
 */
public class StockInitDetailVO {

    private FinStockInitBatch batch;
    private List<FinStockInitItem> items;

    public FinStockInitBatch getBatch() { return batch; }
    public void setBatch(FinStockInitBatch batch) { this.batch = batch; }

    public List<FinStockInitItem> getItems() { return items; }
    public void setItems(List<FinStockInitItem> items) { this.items = items; }
}
