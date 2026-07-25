package com.junsong.finance.service;

import java.util.List;
import com.junsong.finance.domain.FinStocktake;
import com.junsong.finance.domain.vo.StocktakeApprovalRequest;
import com.junsong.finance.domain.vo.StocktakeAssignRequest;
import com.junsong.finance.domain.vo.StocktakeCountRequest;
import com.junsong.finance.domain.vo.StocktakeCreateRequest;
import com.junsong.finance.domain.vo.StocktakeDetailVO;
import com.junsong.finance.domain.vo.StocktakeQuery;
import com.junsong.finance.domain.vo.StocktakeRecountRequest;
import com.junsong.finance.domain.vo.StocktakeReverseRequest;

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
 * Task 5 范围：submit / recount / approve（提交、阈值复盘与审批）
 * 后续 Task 扩展：post / cancel / reverse
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

    /**
     * 提交盘点任务（COUNTING → SUBMITTED 或 RECOUNTING）。
     *
     * 安全契约：
     * 1. 仅 COUNTING 状态任务允许提交
     * 2. 所有行必须已录入 actualQuantity
     * 3. 计算 adjustedExpected = expectedQuantity + movementAfterFreeze（Task 5 中 movement=0）
     * 4. 计算临时方差 variance = actual - adjustedExpected
     * 5. 当任一行 |variance| > recount.quantityThreshold 时，且任务已分配 recountUserId，流转至 RECOUNTING
     * 6. 否则流转至 SUBMITTED
     * 7. 不更新库存/成本（仅更新行表的临时方差字段）
     *
     * @param stocktakeId 盘点任务ID
     * @param version 头表版本号（乐观锁）
     * @return 影响行数
     */
    int submitStocktake(Long stocktakeId, Integer version);

    /**
     * 盘点行复盘录入（RECOUNTING 状态）。
     *
     * 安全契约：
     * 1. 仅 RECOUNTING 状态任务允许录入复盘
     * 2. 非 admin 时仅分配的 recountUserId 可录入
     * 3. recountUserId 必须与 counterUserId 不同
     * 4. recountQuantity 非负
     * 5. 幂等键非空；相同键相同负载返回原结果；不同负载拒绝
     * 6. 复盘方差非零时 reasonCode 和 reason 必填
     * 7. 不更新库存/成本
     *
     * @param stocktakeId 盘点任务ID
     * @param itemId 行表ID
     * @param request 复盘请求
     * @return 影响行数（幂等重放时返回 1，但不重复更新）
     */
    int recountItem(Long stocktakeId, Long itemId, StocktakeRecountRequest request);

    /**
     * 审批盘点任务（SUBMITTED/RECOUNTING → APPROVED 或 COUNTING）。
     *
     * 安全契约：
     * 1. 仅 SUBMITTED 或 RECOUNTING 状态任务允许审批
     * 2. decision=APPROVE → APPROVED；decision=REJECT → COUNTING（驳回重盘）
     * 3. 审批人不能是盘点人（counter）或复盘人（recountUser）
     * 4. 审批通过时确定 finalQuantity（优先用 recountQuantity，否则 actualQuantity）
     * 5. 不更新库存/成本（过账在 Task 6）
     *
     * @param stocktakeId 盘点任务ID
     * @param request 审批请求（含 decision/comment/version）
     * @return 影响行数
     */
    int approveStocktake(Long stocktakeId, StocktakeApprovalRequest request);

    /**
     * 过账盘点任务（APPROVED → POSTED）。
     *
     * 安全契约：
     * 1. 仅 APPROVED 状态任务允许过账
     * 2. 会计期间必须为 ACTIVE（0）
     * 3. 幂等：takeNo 已有库存流水则拒绝重复过账
     * 4. 对每个行按 (deptId, productId) 排序后锁定 position 行（避免死锁）
     * 5. 重新计算 movementAfterFreeze 和 adjustedExpected
     * 6. 重新计算 finalVariance = finalQuantity - adjustedExpected
     * 7. finalVariance > 0（盘盈）→ STOCK_TAKE_GAIN 流水 + applyStocktakeGain
     * 8. finalVariance < 0（盘亏）→ STOCK_TAKE_LOSS 流水 + applyStocktakeLoss
     * 9. finalVariance == 0 → 不写流水
     * 10. 更新行表 unitCost, varianceAmount, stockLedgerId, costLedgerId
     * 11. 数量与成本在同一事务内原子过账；任一步失败全量回滚
     *
     * @param stocktakeId 盘点任务ID
     * @param version 头表版本号（乐观锁）
     * @return 影响行数
     */
    int postStocktake(Long stocktakeId, Integer version);

    /**
     * 取消盘点任务（POSTED 之前的状态 → CANCELLED）。
     *
     * 安全契约：
     * 1. 仅 DRAFT/COUNTING/SUBMITTED/RECOUNTING/APPROVED 状态可取消
     * 2. 已 POSTED / CANCELLED / REVERSED 的任务禁止取消
     * 3. 取消是软删除，不写库存/成本（因为尚未过账）
     * 4. 记录历史（CANCEL action）
     *
     * @param stocktakeId 盘点任务ID
     * @param version 头表版本号（乐观锁）
     * @return 影响行数
     */
    int cancelStocktake(Long stocktakeId, Integer version);

    /**
     * 整单冲销盘点任务（POSTED → REVERSED）。
     *
     * 安全契约：
     * 1. 仅 POSTED 状态可冲销；已 REVERSED 拒绝二次冲销
     * 2. 会计期间必须为 ACTIVE（0）
     * 3. 必须提供 reason 和 idempotencyKey
     * 4. 整单冲销：写入相反的数量与成本流水，不删除原始证据
     * 5. 按 (deptId, productId) 排序锁定行，避免死锁
     * 6. 原始成本复用：从原过账成本流水读取 unitCost
     * 7. 任一步失败全量回滚（事务原子性）
     *
     * @param stocktakeId 盘点任务ID
     * @param request 冲销请求（含 reason/idempotencyKey/version）
     * @return 影响行数
     */
    int reverseStocktake(Long stocktakeId, StocktakeReverseRequest request);
}
