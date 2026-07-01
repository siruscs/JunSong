package com.junsong.open.service;

import java.util.List;
import com.junsong.open.domain.OpenContract;

/**
 * 开放平台合约 服务层接口
 */
public interface IOpenContractService
{
    OpenContract selectOpenContractById(Long id);
    List<OpenContract> selectOpenContractList(OpenContract openContract);
    int insertOpenContract(OpenContract openContract);
    int updateOpenContract(OpenContract openContract);
    int activateContract(Long id);
    int terminateContract(Long id);
    int deleteOpenContractByIds(Long[] ids);
}
