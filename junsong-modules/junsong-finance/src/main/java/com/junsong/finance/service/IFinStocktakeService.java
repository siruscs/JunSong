package com.junsong.finance.service;

import java.util.List;
import com.junsong.finance.domain.FinStocktake;
import com.junsong.finance.domain.vo.StocktakeAssignRequest;
import com.junsong.finance.domain.vo.StocktakeCountRequest;
import com.junsong.finance.domain.vo.StocktakeCreateRequest;
import com.junsong.finance.domain.vo.StocktakeDetailVO;
import com.junsong.finance.domain.vo.StocktakeQuery;

/**
 * 库存盘点 Service 接口。
 *
 * 安全契约：
 * 1. 所有方法从 TenantContext 获取租户ID，缺失即拒绝
 * 2. 所有读写按授权部门集合过滤（admin 跳过；非 admin 与 RemoteUserService.getUserDeptList 求交集）
 * 3. 状态流转使用乐观锁 version 谓词
 * 4. 创建时冻结期望数量（从 fin_stock_position 无锁读取）
 * 5. counter 视角隐藏期望值（盲盘保护）
 *
 * Task 3 范围：create / list / detail / assign
 * Task 4 范围：start / count（盲盘与幂等行录入）
 * 后续 Task 扩展：submit / recount / approve / post / cancel / reverse
 *
 * @author junsong
 */
public interface IFinStocktakeService {

    /**
     * 创建盘点任务并冻结期望数量。
     *
     * @param request 创建请求
     * @return 盘点任务ID
     */
    Long createStocktake(StocktakeCreateRequest request);

    /**
     * 分页查询盘点任务列表（按授权部门过滤）。
     *
     * @param query 查询参数
     * @return 任务头表列表
     */
    List<FinStocktake> listStocktakes(StocktakeQuery query);

    /**
     * 查询盘点任务详情（头表 + 行表 + 历史）。
     * counter 视角且任务未提交时，隐藏期望值（盲盘保护）。
     *
     * @param stocktakeId 盘点任务ID
     * @return 详情视图
     */
    StocktakeDetailVO getStocktakeDetail(Long stocktakeId);

    /**
     * 分配盘点人和复盘人（仅 DRAFT 状态允许）。
     *
     * @param stocktakeId 盘点任务ID
     * @param request 分配请求（含乐观锁 version）
     * @return 影响行数
     */
    int assignCounter(Long stocktakeId, StocktakeAssignRequest request);

    /**
     * 启动盘点任务（DRAFT → COUNTING）。
     * 仅已分配盘点人的草稿任务可启动。
     *
     * @param stocktakeId 盘点任务ID
     * @param version 头表版本号（乐观锁）
     * @return 影响行数
     */
    int startStocktake(Long stocktakeId, Integer version);

    /**
     * 盘点行录入（盲盘 + 幂等）。
     *
     * 安全契约：
     * 1. 仅 COUNTING 状态任务允许录入
     * 2. 非 admin 时仅分配的 counter 可录入
     * 3. actualQuantity 非负
     * 4. 幂等键非空；相同键相同负载返回原结果；相同键不同负载拒绝
     * 5. 方差非零时 reasonCode 和 reason 必填
     * 6. 不更新库存/成本（仅存储盘点证据）
     *
     * @param stocktakeId 盘点任务ID
     * @param itemId 行表ID
     * @param request 录入请求
     * @return 影响行数（幂等重放时返回 1，但不重复更新）
     */
    int countItem(Long stocktakeId, Long itemId, StocktakeCountRequest request);
}
