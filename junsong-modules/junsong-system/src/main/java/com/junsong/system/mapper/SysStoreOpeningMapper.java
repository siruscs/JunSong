package com.junsong.system.mapper;

import com.junsong.system.domain.SysStoreOpening;
import java.util.List;

public interface SysStoreOpeningMapper
{
    SysStoreOpening selectStoreOpeningById(Long id);

    SysStoreOpening selectStoreOpeningByOrderNo(String orderNo);

    SysStoreOpening selectStoreOpeningByProcessInstanceId(String processInstanceId);

    List<SysStoreOpening> selectStoreOpeningList(SysStoreOpening query);

    int insertStoreOpening(SysStoreOpening storeOpening);

    int updateStoreOpening(SysStoreOpening storeOpening);

    int updateWorkflowSnapshot(SysStoreOpening storeOpening);

    int deleteStoreOpeningById(Long id);
}
