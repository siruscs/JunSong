package com.junsong.finance.mapper;

import com.junsong.finance.domain.FinanceReceivableCollection;
import com.junsong.finance.domain.FinanceReceivableCollectionLog;
import com.junsong.finance.domain.vo.ReceivableCollectionRowVO;
import com.junsong.finance.domain.vo.ReceivableCollectionSyncParams;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FinanceReceivableCollectionMapper {
    FinanceReceivableCollection selectById(Long collectionId);

    FinanceReceivableCollection selectBySaleId(Long saleId);

    List<ReceivableCollectionRowVO> selectDashboardRows(ReceivableCollectionSyncParams params);

    List<ReceivableCollectionRowVO> selectList(ReceivableCollectionSyncParams params);

    List<ReceivableCollectionRowVO> selectUnpaidSalesForSync(ReceivableCollectionSyncParams params);

    int insertCollection(FinanceReceivableCollection collection);

    int updateCollection(FinanceReceivableCollection collection);

    int insertLog(FinanceReceivableCollectionLog log);

    int refreshCollectionAmounts(@Param("collectionId") Long collectionId);
}
