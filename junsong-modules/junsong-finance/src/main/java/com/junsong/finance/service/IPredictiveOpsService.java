package com.junsong.finance.service;

import com.junsong.finance.domain.vo.PredictiveOpsDashboardVO;
import com.junsong.finance.domain.vo.PredictiveOpsQueryParams;
import com.junsong.finance.domain.vo.WhatIfSimulationParams;
import com.junsong.finance.domain.vo.WhatIfSimulationResultVO;

public interface IPredictiveOpsService {

    /**
     * 计算 R24 预测辅助 V2 仪表盘。
     */
    PredictiveOpsDashboardVO getDashboard(PredictiveOpsQueryParams params);

    /**
     * 持久化预测样本与解释因子（只读快照）。
     */
    int createSnapshot(PredictiveOpsQueryParams params);

    /**
     * what-if 模拟（只读，不修改任何业务表）。
     */
    WhatIfSimulationResultVO simulateWhatIf(WhatIfSimulationParams params);
}
