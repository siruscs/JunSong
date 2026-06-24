package com.junsong.workflow.lowcode.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import com.junsong.workflow.lowcode.domain.LcBizNodeTimer;

/**
 * 低代码节点定时器配置 Mapper
 *
 * @author junsong
 */
@Mapper
public interface LcBizNodeTimerMapper
{
    /**
     * 查询节点定时器列表
     */
    public List<LcBizNodeTimer> selectLcBizNodeTimerList(LcBizNodeTimer query);

    /**
     * 按 ID 查询
     */
    public LcBizNodeTimer selectLcBizNodeTimerById(Long id);

    /**
     * 按业务编码查询所有定时器配置
     */
    public List<LcBizNodeTimer> selectByBizCode(String bizCode);

    /**
     * 新增
     */
    public int insertLcBizNodeTimer(LcBizNodeTimer timer);

    /**
     * 修改
     */
    public int updateLcBizNodeTimer(LcBizNodeTimer timer);

    /**
     * 逻辑删除（批量）
     */
    public int deleteLcBizNodeTimerByIds(Long[] ids);

    /**
     * 物理删除（按 bizCode，配置重建时用）
     */
    public int physicalDeleteByBizCode(String bizCode);
}
