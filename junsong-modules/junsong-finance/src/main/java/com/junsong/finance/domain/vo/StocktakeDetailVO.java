package com.junsong.finance.domain.vo;

import java.util.List;
import com.junsong.finance.domain.FinStocktake;
import com.junsong.finance.domain.FinStocktakeHistory;
import com.junsong.finance.domain.FinStocktakeItem;

/**
 * 库存盘点任务详情视图（头表 + 行表 + 历史）。
 *
 * 盲盘保护：
 * - 当 hideExpected=true（counter 视角且任务未提交）时，items 中的
 *   expectedQuantity / adjustedExpectedQuantity / varianceQuantity / varianceAmount / unitCost
 *   字段会被置 null，避免盘点人看到期望值。
 * - 管理员/审批人视角或任务已提交后，hideExpected=false，返回完整数据。
 *
 * @author junsong
 */
public class StocktakeDetailVO {

    private FinStocktake stocktake;
    private List<FinStocktakeItem> items;
    private List<FinStocktakeHistory> history;
    private boolean hideExpected;

    public FinStocktake getStocktake() { return stocktake; }
    public void setStocktake(FinStocktake stocktake) { this.stocktake = stocktake; }

    public List<FinStocktakeItem> getItems() { return items; }
    public void setItems(List<FinStocktakeItem> items) { this.items = items; }

    public List<FinStocktakeHistory> getHistory() { return history; }
    public void setHistory(List<FinStocktakeHistory> history) { this.history = history; }

    public boolean isHideExpected() { return hideExpected; }
    public void setHideExpected(boolean hideExpected) { this.hideExpected = hideExpected; }
}
