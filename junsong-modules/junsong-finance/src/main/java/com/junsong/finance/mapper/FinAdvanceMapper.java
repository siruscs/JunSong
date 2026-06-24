package com.junsong.finance.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.junsong.finance.domain.FinAdvance;

/**
 * 借支记录Mapper接口
 * 
 * @author junsong
 */
public interface FinAdvanceMapper
{
    /**
     * 查询借支记录
     * 
     * @param advanceId 借支记录主键
     * @return 借支记录
     */
    public FinAdvance selectFinAdvanceByAdvanceId(Long advanceId);

    /**
     * 查询借支记录列表
     * 
     * @param finAdvance 借支记录
     * @return 借支记录集合
     */
    public List<FinAdvance> selectFinAdvanceList(FinAdvance finAdvance);

    /**
     * 新增借支记录
     * 
     * @param finAdvance 借支记录
     * @return 结果
     */
    public int insertFinAdvance(FinAdvance finAdvance);

    /**
     * 修改借支记录
     * 
     * @param finAdvance 借支记录
     * @return 结果
     */
    public int updateFinAdvance(FinAdvance finAdvance);

    /**
     * 删除借支记录
     * 
     * @param advanceId 借支记录主键
     * @return 结果
     */
    public int deleteFinAdvanceByAdvanceId(Long advanceId);

    /**
     * 批量删除借支记录
     * 
     * @param advanceIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteFinAdvanceByAdvanceIds(Long[] advanceIds);

    /**
     * 校验借支单号是否唯一
     * 
     * @param advanceNo 借支单号
     * @return 结果
     */
    public FinAdvance checkAdvanceNoUnique(String advanceNo);

    /**
     * 统计今日借支单数量
     * 
     * @return 结果
     */
    public int countTodayAdvances();

    /**
     * 统计未核销借支总金额
     * 
     * @return 结果
     */
    public java.math.BigDecimal sumUnverifiedAdvances();
    public java.math.BigDecimal sumUnverifiedAdvancesByDeptId(@Param("deptId") Long deptId);

    /**
     * 根据借支记录ID数组查询借支记录
     * 
     * @param advanceIds 借支记录ID数组
     * @return 借支记录集合
     */
    public List<FinAdvance> selectFinAdvanceByAdvanceIds(Long[] advanceIds);
}
