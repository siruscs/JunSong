package com.junsong.member.mapper;

import java.util.List;
import com.junsong.member.domain.MemPointsRecord;

/**
 * 积分记录Mapper接口
 *
 * @author junsong
 */
public interface MemPointsRecordMapper
{
    /**
     * 查询积分记录
     *
     * @param recordId 记录ID
     * @return 积分记录
     */
    public MemPointsRecord selectMemPointsRecordByRecordId(Long recordId);

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
     * @param recordId 记录ID
     * @return 结果
     */
    public int deleteMemPointsRecordByRecordId(Long recordId);

    /**
     * 批量删除积分记录
     *
     * @param recordIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteMemPointsRecordByRecordIds(Long[] recordIds);

    /**
     * 校验记录编号是否唯一
     *
     * @param recordNo 记录编号
     * @return 结果
     */
    public MemPointsRecord checkRecordNoUnique(String recordNo);

    public List<MemPointsRecord> selectMemPointsRecordByRemark(String remark);

    public MemPointsRecord selectLatestBalanceByMemberId(Long memberId);
}
