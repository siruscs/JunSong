package com.junsong.member.service;

import java.util.List;
import com.junsong.member.domain.MemPointsRecord;

/**
 * 积分记录Service接口
 */
public interface IMemPointsRecordService {

    /**
     * 查询积分记录
     *
     * @param id 积分记录ID
     * @return 积分记录
     */
    public MemPointsRecord selectMemPointsRecordById(Long id);

    /**
     * 查询积分记录列表
     *
     * @param memPointsRecord 积分记录
     * @return 积分记录集合
     */
    public List<MemPointsRecord> selectMemPointsRecordList(MemPointsRecord memPointsRecord);

    /**
     * 新增积分记录
     *
     * @param memPointsRecord 积分记录
     * @return 结果
     */
    public int insertMemPointsRecord(MemPointsRecord memPointsRecord);

    /**
     * 修改积分记录
     *
     * @param memPointsRecord 积分记录
     * @return 结果
     */
    public int updateMemPointsRecord(MemPointsRecord memPointsRecord);

    /**
     * 删除积分记录
     *
     * @param id 积分记录ID
     * @return 结果
     */
    public int deleteMemPointsRecordById(Long id);

    /**
     * 批量删除积分记录
     *
     * @param ids 需要删除的数据ID
     * @return 结果
     */
    public int deleteMemPointsRecordByIds(Long[] ids);

    /**
     * 校验积分记录编号是否唯一
     *
     * @param memPointsRecord 积分记录
     * @return 结果
     */
    public boolean checkMemPointsRecordNoUnique(MemPointsRecord memPointsRecord);

    public List<MemPointsRecord> selectMemPointsRecordByRemark(String remark);

    public MemPointsRecord selectLatestBalanceByMemberId(Long memberId);
}
