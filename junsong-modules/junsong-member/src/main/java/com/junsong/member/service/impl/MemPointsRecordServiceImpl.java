package com.junsong.member.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.junsong.member.mapper.MemPointsRecordMapper;
import com.junsong.member.domain.MemPointsRecord;
import com.junsong.member.service.IMemPointsRecordService;
import com.junsong.common.datascope.annotation.DataScope;

/**
 * 积分记录Service业务层处理
 */
@Service
public class MemPointsRecordServiceImpl implements IMemPointsRecordService {

    @Autowired
    private MemPointsRecordMapper memPointsRecordMapper;

    /**
     * 查询积分记录
     *
     * @param id 积分记录ID
     * @return 积分记录
     */
    @Override
    @DataScope(deptAlias = "mem_points_record")
    public MemPointsRecord selectMemPointsRecordById(Long id) {
        return memPointsRecordMapper.selectMemPointsRecordByRecordId(id);
    }

    /**
     * 查询积分记录列表
     *
     * @param memPointsRecord 积分记录
     * @return 积分记录
     */
    @Override
    @DataScope(deptAlias = "mem_points_record")
    public List<MemPointsRecord> selectMemPointsRecordList(MemPointsRecord memPointsRecord) {
        return memPointsRecordMapper.selectMemPointsRecordList(memPointsRecord);
    }

    /**
     * 新增积分记录
     *
     * @param memPointsRecord 积分记录
     * @return 结果
     */
    @Override
    @Transactional
    public int insertMemPointsRecord(MemPointsRecord memPointsRecord) {
        return memPointsRecordMapper.insertMemPointsRecord(memPointsRecord);
    }

    /**
     * 修改积分记录
     *
     * @param memPointsRecord 积分记录
     * @return 结果
     */
    @Override
    @Transactional
    public int updateMemPointsRecord(MemPointsRecord memPointsRecord) {
        return memPointsRecordMapper.updateMemPointsRecord(memPointsRecord);
    }

    /**
     * 删除积分记录
     *
     * @param id 积分记录ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteMemPointsRecordById(Long id) {
        return memPointsRecordMapper.deleteMemPointsRecordByRecordId(id);
    }

    /**
     * 批量删除积分记录
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteMemPointsRecordByIds(Long[] ids) {
        return memPointsRecordMapper.deleteMemPointsRecordByRecordIds(ids);
    }

    /**
     * 校验积分记录编号是否唯一
     *
     * @param memPointsRecord 积分记录
     * @return 结果
     */
    @Override
    public boolean checkMemPointsRecordNoUnique(MemPointsRecord memPointsRecord)
    {
        // 积分记录没有编号字段，直接返回true表示不冲突
        return true;
    }

    @Override
    public List<MemPointsRecord> selectMemPointsRecordByRemark(String remark) {
        return memPointsRecordMapper.selectMemPointsRecordByRemark(remark);
    }

    @Override
    public MemPointsRecord selectLatestBalanceByMemberId(Long memberId) {
        return memPointsRecordMapper.selectLatestBalanceByMemberId(memberId);
    }
}
