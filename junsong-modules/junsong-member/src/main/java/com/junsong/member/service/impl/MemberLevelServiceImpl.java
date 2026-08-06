package com.junsong.member.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.junsong.member.domain.MemMemberCardType;
import com.junsong.member.mapper.MemMemberCardTypeMapper;
import com.junsong.member.service.IMemberLevelService;
import com.junsong.common.core.context.TenantContext;
import com.junsong.common.security.utils.SecurityUtils;

/**
 * 会员等级配置Service实现
 *
 * @author junsong
 */
@Service
public class MemberLevelServiceImpl implements IMemberLevelService
{
    @Autowired
    private MemMemberCardTypeMapper cardTypeMapper;

    @Override
    public List<MemMemberCardType> selectLevelList(MemMemberCardType cardType)
    {
        return cardTypeMapper.selectCardTypeList(cardType);
    }

    @Override
    public MemMemberCardType selectLevelByTypeCode(String typeCode)
    {
        return cardTypeMapper.selectCardTypeByTypeCode(typeCode, tenantId(), deptId());
    }

    @Override
    public List<MemMemberCardType> selectEnabledLevels()
    {
        return cardTypeMapper.selectEnabledCardTypes(tenantId(), deptId());
    }

    @Override
    public int updateLevel(MemMemberCardType cardType)
    {
        int rows = cardTypeMapper.updateCardType(cardType);
        if (rows == 1 || cardType.getDeptId() == null || cardType.getDeptId() == 0) return rows;
        MemMemberCardType baseline = cardTypeMapper.selectCardTypeById(cardType.getTypeId(), tenantId(), 0L);
        if (baseline == null) return rows;
        cardType.setTypeCode(baseline.getTypeCode());
        return cardTypeMapper.insertCardType(cardType);
    }

    @Override
    public int insertLevel(MemMemberCardType cardType)
    {
        return cardTypeMapper.insertCardType(cardType);
    }

    @Override
    public boolean checkTypeCodeUnique(MemMemberCardType cardType)
    {
        Long typeId = cardType.getTypeId();
        MemMemberCardType existing = cardTypeMapper.checkTypeCodeUnique(cardType.getTypeCode(), tenantId(),
                cardType.getDeptId() == null ? deptId() : cardType.getDeptId());
        return existing == null || existing.getTypeId().equals(typeId);
    }

    @Override
    public String calculateLevel(Long growthValue)
    {
        if (growthValue == null || growthValue <= 0)
        {
            return "experience";
        }
        MemMemberCardType target = cardTypeMapper.selectLevelByGrowth(growthValue, tenantId(), deptId());
        return target != null ? target.getTypeCode() : "experience";
    }

    private Long tenantId() { return TenantContext.getTenantId(); }

    private Long deptId()
    {
        try { return SecurityUtils.getDeptId(); } catch (Exception e) { return 0L; }
    }
}
