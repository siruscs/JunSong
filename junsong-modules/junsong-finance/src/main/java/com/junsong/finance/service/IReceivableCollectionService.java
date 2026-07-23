package com.junsong.finance.service;

import com.junsong.finance.domain.vo.ReceivableCollectionDashboardVO;
import com.junsong.finance.domain.vo.ReceivableCollectionRowVO;
import com.junsong.finance.domain.vo.ReceivableCollectionSyncParams;
import com.junsong.finance.domain.vo.ReceivableCollectionUpdateParams;

import java.util.List;

public interface IReceivableCollectionService {
    ReceivableCollectionDashboardVO getDashboard(ReceivableCollectionSyncParams params);

    List<ReceivableCollectionRowVO> list(ReceivableCollectionSyncParams params);

    int syncFromReceivables(ReceivableCollectionSyncParams params);

    int updateFollow(Long collectionId, ReceivableCollectionUpdateParams params);
    boolean canAccess(Long collectionId, Long deptId);
}
