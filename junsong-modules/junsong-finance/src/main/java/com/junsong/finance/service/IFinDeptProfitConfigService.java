package com.junsong.finance.service;

import java.util.List;
import com.junsong.finance.domain.FinDeptProfitConfig;

public interface IFinDeptProfitConfigService
{
    public FinDeptProfitConfig selectFinDeptProfitConfigByConfigId(Long configId);
    public FinDeptProfitConfig selectFinDeptProfitConfigByDeptId(Long deptId);
    public List<FinDeptProfitConfig> selectFinDeptProfitConfigList(FinDeptProfitConfig finDeptProfitConfig);
    public int insertFinDeptProfitConfig(FinDeptProfitConfig finDeptProfitConfig);
    public int updateFinDeptProfitConfig(FinDeptProfitConfig finDeptProfitConfig);
    public int deleteFinDeptProfitConfigByConfigIds(Long[] configIds);
}
