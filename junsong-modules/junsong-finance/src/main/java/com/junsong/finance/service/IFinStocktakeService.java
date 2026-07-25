package com.junsong.finance.service;

import java.util.List;
import com.junsong.finance.domain.FinStocktake;
import com.junsong.finance.domain.vo.StocktakeAssignRequest;
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
 * 后续 Task 扩展：start / count / submit / recount / approve / post / cancel / reverse
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
}
