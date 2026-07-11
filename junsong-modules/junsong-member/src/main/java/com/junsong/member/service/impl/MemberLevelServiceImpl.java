package com.junsong.member.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.junsong.member.domain.MemMemberCardType;
import com.junsong.member.mapper.MemMemberCardTypeMapper;
import com.junsong.member.service.IMemberLevelService;

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
        return cardTypeMapper.selectCardTypeByTypeCode(typeCode);
    }

    @Override
    public List<MemMemberCardType> selectEnabledLevels()
    {
        return cardTypeMapper.selectEnabledCardTypes();
    }

    @Override
    public int updateLevel(MemMemberCardType cardType)
    {
        return cardTypeMapper.updateCardType(cardType);
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
        MemMemberCardType existing = cardTypeMapper.checkTypeCodeUnique(cardType.getTypeCode());
        return existing == null || existing.getTypeId().equals(typeId);
    }

    @Override
    public String calculateLevel(Long growthValue)
    {
        if (growthValue == null || growthValue <= 0)
        {
            return "experience";
        }
        MemMemberCardType target = cardTypeMapper.selectLevelByGrowth(growthValue);
        return target != null ? target.getTypeCode() : "experience";
    }
}
