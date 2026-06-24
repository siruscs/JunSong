package com.junsong.finance.mapper;

import java.util.List;
import com.junsong.finance.domain.FinDeptProfitConfig;

public interface FinDeptProfitConfigMapper
{
    public FinDeptProfitConfig selectFinDeptProfitConfigByConfigId(Long configId);
    public FinDeptProfitConfig selectFinDeptProfitConfigByDeptId(Long deptId);
    public FinDeptProfitConfig selectAnyFinDeptProfitConfigByDeptId(Long deptId);
    public List<FinDeptProfitConfig> selectFinDeptProfitConfigList(FinDeptProfitConfig finDeptProfitConfig);
    public int insertFinDeptProfitConfig(FinDeptProfitConfig finDeptProfitConfig);
    public int updateFinDeptProfitConfig(FinDeptProfitConfig finDeptProfitConfig);
    public int deleteFinDeptProfitConfigByConfigId(Long configId);
    public int deleteFinDeptProfitConfigByConfigIds(Long[] configIds);
}
