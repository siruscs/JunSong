package com.junsong.finance.service.impl;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinStockLedger;
import com.junsong.finance.domain.FinProduct;
import com.junsong.finance.domain.vo.StockTakeRequest;
import com.junsong.finance.mapper.FinProductMapper;
import com.junsong.finance.mapper.FinStockLedgerMapper;
import com.junsong.finance.service.IStockTakeService;

/**
 * 库存盘点服务实现。
 *
 * 安全模型：
 * 1. 租户：TenantContext.getTenantId()，客户端不可设置
 * 2. 部门：SecurityUtils.isAdmin() 不限，否则校验 deptId 在授权范围内
 * 3. 商品：必须属于当前部门（查 fin_product.dept_id）
 * 4. 数量：actualQuantity >= 0，delta = actual - current
 * 5. 原因：delta != 0 时 reason 必填
 * 6. 幂等：takeNo 唯一，重复提交被拒绝
 * 7. 并发：SELECT ... FOR UPDATE 锁定 position 行
 * 8. 原子：流水写入 + position 更新在同一事务
 *
 * @author junsong
 */
@Service
public class StockTakeServiceImpl implements IStockTakeService {

    private static final String STOCK_TAKE_GAIN = "STOCK_TAKE_GAIN";
    private static final String STOCK_TAKE_LOSS = "STOCK_TAKE_LOSS";
    private static final String REF_STOCK_TAKE = "STOCK_TAKE";

    @Autowired
    private FinStockLedgerMapper finStockLedgerMapper;

    @Autowired
    private FinProductMapper finProductMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long recordStockTake(StockTakeRequest request) {
        // 1. 参数校验
        assertRequestValid(request);

        // 2. 租户上下文
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new ServiceException("租户上下文缺失，禁止盘点");
        }

        // 3. 幂等校验：takeNo 已存在则拒绝
        int existing = finStockLedgerMapper.countByReferenceNo(tenantId, request.getTakeNo());
        if (existing > 0) {
            throw new ServiceException("盘点单号已存在，禁止重复提交：" + request.getTakeNo());
        }

        // 4. 部门授权校验
        Long deptId = request.getDeptId();
        assertDeptAuthorized(deptId);

        // 5. 商品归属校验：商品必须属于当前部门
        FinProduct product = finProductMapper.selectFinProductByProductIdAndDeptId(
                request.getProductId(), deptId);
        if (product == null) {
            throw new ServiceException("商品不存在或无权访问（productId=" + request.getProductId()
                    + ", deptId=" + deptId + "）");
        }

        // 6. 加行锁查询当前库存
        finStockLedgerMapper.insertPositionIfAbsent(tenantId, deptId, request.getProductId());
        int current = nz(finStockLedgerMapper.selectPositionQuantityForUpdate(
                tenantId, deptId, request.getProductId()));

        // 7. 计算盘盈/盘亏
        int actual = request.getActualQuantity();
        int delta = actual - current;
        if (delta == 0) {
            // 数量一致，无需写入流水，但仍返回一个虚拟ID表示处理成功
            return 0L;
        }

        // 8. 原因必填（盘盈或盘亏）
        if (StringUtils.isBlank(request.getReason())) {
            throw new ServiceException("盘盈盘亏必须填写原因");
        }

        // 9. 写入流水
        String changeType = delta > 0 ? STOCK_TAKE_GAIN : STOCK_TAKE_LOSS;
        int after = current + delta;
        BigDecimal unitCost = delta > 0 ? request.getUnitCost() : null;

        FinStockLedger ledger = new FinStockLedger();
        ledger.setTenantId(tenantId);
        ledger.setDeptId(deptId);
        ledger.setProductId(request.getProductId());
        ledger.setProductName(product.getProductName());
        ledger.setChangeType(changeType);
        ledger.setChangeQuantity(delta);
        ledger.setBeforeQuantity(current);
        ledger.setAfterQuantity(after);
        ledger.setUnitCost(unitCost);
        ledger.setReferenceType(REF_STOCK_TAKE);
        ledger.setReferenceNo(request.getTakeNo());
        ledger.setDelFlag("0");
        ledger.setCreateBy(SecurityUtils.getUsername());
        ledger.setRemark(request.getReason());
        finStockLedgerMapper.insertFinStockLedger(ledger);

        // 10. 更新结存
        int affected = finStockLedgerMapper.updatePositionQuantity(
                tenantId, deptId, request.getProductId(), after);
        if (affected != 1) {
            throw new ServiceException("库存结存更新影响行数异常（" + affected + "），事务回滚");
        }

        return ledger.getLedgerId();
    }

    private void assertRequestValid(StockTakeRequest request) {
        if (request == null) {
            throw new ServiceException("盘点请求不能为空");
        }
        if (StringUtils.isBlank(request.getTakeNo())) {
            throw new ServiceException("盘点单号不能为空");
        }
        if (request.getDeptId() == null) {
            throw new ServiceException("门店ID不能为空");
        }
        if (request.getProductId() == null) {
            throw new ServiceException("商品ID不能为空");
        }
        if (request.getActualQuantity() == null || request.getActualQuantity() < 0) {
            throw new ServiceException("盘点数量不能为负数");
        }
    }

    private void assertDeptAuthorized(Long deptId) {
        if (SecurityUtils.isAdmin()) {
            return;
        }
        Long currentDeptId = SecurityUtils.getDeptId();
        if (currentDeptId == null || !currentDeptId.equals(deptId)) {
            throw new ServiceException("无权盘点该门店（deptId=" + deptId + "）");
        }
    }

    private int nz(Integer value) {
        return value != null ? value : 0;
    }
}
