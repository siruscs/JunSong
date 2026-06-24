package com.junsong.member.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.member.mapper.MemPointsGoodsMapper;
import com.junsong.member.domain.MemPointsGoods;
import com.junsong.member.service.IMemPointsGoodsService;
import com.junsong.common.datascope.annotation.DataScope;

/**
 * 积分物品Service业务层处理
 */
@Service
public class MemPointsGoodsServiceImpl implements IMemPointsGoodsService {

    @Autowired
    private MemPointsGoodsMapper memPointsGoodsMapper;

    /**
     * 查询积分物品
     *
     * @param id 积分物品ID
     * @return 积分物品
     */
    @Override
    @DataScope(deptAlias = "mem_points_goods")
    public MemPointsGoods selectMemPointsGoodsById(Long id) {
        return memPointsGoodsMapper.selectMemPointsGoodsById(id);
    }

    /**
     * 查询积分物品列表
     *
     * @param memPointsGoods 积分物品
     * @return 积分物品
     */
    @Override
    @DataScope(deptAlias = "mem_points_goods")
    public List<MemPointsGoods> selectMemPointsGoodsList(MemPointsGoods memPointsGoods) {
        return memPointsGoodsMapper.selectMemPointsGoodsList(memPointsGoods);
    }

    /**
     * 新增积分物品
     *
     * @param memPointsGoods 积分物品
     * @return 结果
     */
    @Override
    @Transactional
    public int insertMemPointsGoods(MemPointsGoods memPointsGoods) {
        return memPointsGoodsMapper.insertMemPointsGoods(memPointsGoods);
    }

    /**
     * 修改积分物品
     *
     * @param memPointsGoods 积分物品
     * @return 结果
     */
    @Override
    @Transactional
    public int updateMemPointsGoods(MemPointsGoods memPointsGoods) {
        return memPointsGoodsMapper.updateMemPointsGoods(memPointsGoods);
    }

    /**
     * 删除积分物品
     *
     * @param id 积分物品ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteMemPointsGoodsById(Long id) {
        return memPointsGoodsMapper.deleteMemPointsGoodsById(id);
    }

    /**
     * 批量删除积分物品
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteMemPointsGoodsByIds(Long[] ids) {
        return memPointsGoodsMapper.deleteMemPointsGoodsByIds(ids);
    }

    /**
     * 校验积分物品编号是否唯一
     *
     * @param memPointsGoods 积分物品
     * @return 结果
     */
    @Override
    public boolean checkMemPointsGoodsNoUnique(MemPointsGoods memPointsGoods) {
        int count = memPointsGoodsMapper.checkMemGoodsCodeUnique(memPointsGoods.getGoodsCode());
        return count == 0;
    }
}
