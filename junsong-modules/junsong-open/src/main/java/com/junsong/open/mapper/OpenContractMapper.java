package com.junsong.open.mapper;

import java.util.List;
import com.junsong.open.domain.OpenContract;

/**
 * 开放平台合约 Mapper接口
 */
public interface OpenContractMapper
{
    OpenContract selectOpenContractById(Long id);
    List<OpenContract> selectOpenContractList(OpenContract openContract);
    int insertOpenContract(OpenContract openContract);
    int updateOpenContract(OpenContract openContract);
    int deleteOpenContractByIds(Long[] ids);
}
