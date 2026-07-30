package com.junsong.finance.service;

import java.util.List;
import com.junsong.finance.domain.FinStockInitBatch;
import com.junsong.finance.domain.vo.StockInitApproveRequest;
import com.junsong.finance.domain.vo.StockInitCreateRequest;
import com.junsong.finance.domain.vo.StockInitDetailVO;
import com.junsong.finance.domain.vo.StockInitPostRequest;
import com.junsong.finance.domain.vo.StockInitQuery;

/**
 * 期初库存 Service 接口。
 *
 * 状态机：DRAFT → VALIDATED → SUBMITTED → APPROVED → POSTED
 *
 * 安全契约：
 * 1. 所有方法从 TenantContext 获取租户ID，缺失即拒绝
 * 2. 所有读写按授权部门集合过滤
 * 3. 状态流转使用乐观锁 version 谓词
 * 4. 过账幂等键 post_idempotency_key 租户内唯一
 * 5. batchNo 服务端生成，不接受客户端传入
 *
 * @author junsong
 */
public interface IFinStockInitService {

    /**
     * 创建期初库存批次（DRAFT）。
     *
     * @param request 创建请求
     * @return 批次ID
     */
    Long createStockInit(StockInitCreateRequest request);

    /**
     * 校验期初库存批次（DRAFT → VALIDATED）。
     *
     * @param batchId 批次ID
     * @param version 头表版本号
     * @return 影响行数
     */
    int validateStockInit(Long batchId, Integer version);

    /**
     * 提交期初库存批次（VALIDATED → SUBMITTED）。
     *
     * @param batchId 批次ID
     * @param version 头表版本号
     * @return 影响行数
     */
    int submitStockInit(Long batchId, Integer version);

    /**
     * 审批期初库存批次（SUBMITTED → APPROVED 或 REJECT → DRAFT）。
     *
     * @param batchId 批次ID
     * @param request 审批请求
     * @return 影响行数
     */
    int approveStockInit(Long batchId, StockInitApproveRequest request);

    /**
     * 过账期初库存批次（APPROVED → POSTED，幂等）。
     *
     * @param batchId 批次ID
     * @param request 过账请求（含幂等键）
     * @return 影响行数
     */
    int postStockInit(Long batchId, StockInitPostRequest request);

    /**
     * 查询期初库存批次详情。
     *
     * @param batchId 批次ID
     * @return 详情视图
     */
    StockInitDetailVO getStockInitDetail(Long batchId);

    /**
     * 列表查询期初库存批次。
     *
     * @param query 查询参数
     * @return 批次列表
     */
    List<FinStockInitBatch> listStockInit(StockInitQuery query);
}
