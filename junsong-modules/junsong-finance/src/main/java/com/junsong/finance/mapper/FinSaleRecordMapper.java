package com.junsong.finance.mapper;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.junsong.finance.domain.FinSaleRecord;

/**
 * 销售记录Mapper接口
 * 
 * @author junsong
 */
public interface FinSaleRecordMapper
{
    /**
     * 查询销售记录
     * 
     * @param saleId 销售记录主键
     * @return 销售记录
     */
    public FinSaleRecord selectFinSaleRecordBySaleId(Long saleId);

    /**
     * 查询销售记录列表
     * 
     * @param finSaleRecord 销售记录
     * @return 销售记录集合
     */
    public List<FinSaleRecord> selectFinSaleRecordList(FinSaleRecord finSaleRecord);

    /**
     * 新增销售记录
     * 
     * @param finSaleRecord 销售记录
     * @return 结果
     */
    public int insertFinSaleRecord(FinSaleRecord finSaleRecord);

    /**
     * 修改销售记录
     * 
     * @param finSaleRecord 销售记录
     * @return 结果
     */
    public int updateFinSaleRecord(FinSaleRecord finSaleRecord);

    /**
     * 删除销售记录
     * 
     * @param saleId 销售记录主键
     * @return 结果
     */
    public int deleteFinSaleRecordBySaleId(Long saleId);

    /**
     * 批量删除销售记录
     * 
     * @param saleIds 需要删除的数据主键集合
     * @return 结果
     */
    public int deleteFinSaleRecordBySaleIds(Long[] saleIds);

    public List<Map<String, Object>> selectSaleTrendStats(@Param("deptIds") List<Long> deptIds, @Param("startTime") Date startTime, @Param("endTime") Date endTime);

    /**
     * 校验销售单号是否唯一
     * 
     * @param saleNo 销售单号
     * @return 结果
     */
    public FinSaleRecord checkSaleNoUnique(String saleNo);

    /**
     * 统计今日销售单数量
     * 
     * @return 结果
     */
    public int countTodaySales();
}
