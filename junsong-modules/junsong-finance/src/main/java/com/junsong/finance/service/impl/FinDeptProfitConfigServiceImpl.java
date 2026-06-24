package com.junsong.finance.service.impl;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinDeptProfitConfig;
import com.junsong.finance.mapper.FinDeptProfitConfigMapper;
import com.junsong.finance.service.IFinDeptProfitConfigService;

@Service
public class FinDeptProfitConfigServiceImpl implements IFinDeptProfitConfigService
{
    @Autowired
    private FinDeptProfitConfigMapper finDeptProfitConfigMapper;

    public FinDeptProfitConfig selectFinDeptProfitConfigByConfigId(Long configId) { return finDeptProfitConfigMapper.selectFinDeptProfitConfigByConfigId(configId); }
    public FinDeptProfitConfig selectFinDeptProfitConfigByDeptId(Long deptId) { return finDeptProfitConfigMapper.selectFinDeptProfitConfigByDeptId(deptId); }
    public List<FinDeptProfitConfig> selectFinDeptProfitConfigList(FinDeptProfitConfig finDeptProfitConfig) { return finDeptProfitConfigMapper.selectFinDeptProfitConfigList(finDeptProfitConfig); }
    public int insertFinDeptProfitConfig(FinDeptProfitConfig finDeptProfitConfig) {
        validateConfig(finDeptProfitConfig);
        fillDefaults(finDeptProfitConfig);
        FinDeptProfitConfig existing = finDeptProfitConfigMapper.selectAnyFinDeptProfitConfigByDeptId(finDeptProfitConfig.getDeptId());
        if (existing != null) {
            if ("0".equals(existing.getDelFlag())) {
                throw new ServiceException("该机构已存在店面分润配置");
            }
            finDeptProfitConfig.setConfigId(existing.getConfigId());
            finDeptProfitConfig.setDelFlag("0");
            finDeptProfitConfig.setUpdateBy(SecurityUtils.getUsername());
            return finDeptProfitConfigMapper.updateFinDeptProfitConfig(finDeptProfitConfig);
        }
        return finDeptProfitConfigMapper.insertFinDeptProfitConfig(finDeptProfitConfig);
    }
    public int updateFinDeptProfitConfig(FinDeptProfitConfig finDeptProfitConfig) {
        validateConfig(finDeptProfitConfig);
        fillDefaults(finDeptProfitConfig);
        return finDeptProfitConfigMapper.updateFinDeptProfitConfig(finDeptProfitConfig);
    }
    public int deleteFinDeptProfitConfigByConfigIds(Long[] configIds) { return finDeptProfitConfigMapper.deleteFinDeptProfitConfigByConfigIds(configIds); }

    private void validateConfig(FinDeptProfitConfig config) {
        if (config.getDeptId() == null) {
            throw new ServiceException("机构不能为空");
        }
        BigDecimal rate = config.getManagerProfitRate();
        if (rate == null) {
            throw new ServiceException("店长分润比例不能为空");
        }
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(new BigDecimal("100")) > 0) {
            throw new ServiceException("店长分润比例必须在0到100之间");
        }
    }

    private void fillDefaults(FinDeptProfitConfig config) {
        if (StringUtils.isEmpty(config.getAutoCreateInvestorPayment())) {
            config.setAutoCreateInvestorPayment("1");
        }
        if (StringUtils.isEmpty(config.getStatus())) {
            config.setStatus("0");
        }
    }
}
