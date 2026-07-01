package com.junsong.open.service.impl;

import java.util.List;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.junsong.open.domain.OpenContract;
import com.junsong.open.mapper.OpenContractMapper;
import com.junsong.open.service.IOpenContractService;

/**
 * 开放平台合约 服务层实现
 */
@Service
public class OpenContractServiceImpl implements IOpenContractService
{
    @Autowired
    private OpenContractMapper openContractMapper;

    @Override
    public OpenContract selectOpenContractById(Long id)
    {
        return openContractMapper.selectOpenContractById(id);
    }

    @Override
    public List<OpenContract> selectOpenContractList(OpenContract openContract)
    {
        return openContractMapper.selectOpenContractList(openContract);
    }

    @Override
    public int insertOpenContract(OpenContract openContract)
    {
        if (openContract.getContractNo() == null || openContract.getContractNo().isEmpty())
        {
            openContract.setContractNo("CT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        }
        if (openContract.getStatus() == null)
        {
            openContract.setStatus("DRAFT");
        }
        return openContractMapper.insertOpenContract(openContract);
    }

    @Override
    public int updateOpenContract(OpenContract openContract)
    {
        return openContractMapper.updateOpenContract(openContract);
    }

    @Override
    public int activateContract(Long id)
    {
        OpenContract contract = new OpenContract();
        contract.setId(id);
        contract.setStatus("ACTIVE");
        return openContractMapper.updateOpenContract(contract);
    }

    @Override
    public int terminateContract(Long id)
    {
        OpenContract contract = new OpenContract();
        contract.setId(id);
        contract.setStatus("TERMINATED");
        return openContractMapper.updateOpenContract(contract);
    }

    @Override
    public int deleteOpenContractByIds(Long[] ids)
    {
        return openContractMapper.deleteOpenContractByIds(ids);
    }
}
