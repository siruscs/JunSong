package com.junsong.workflow.lowcode.service;

import com.junsong.workflow.lowcode.domain.LcBizField;
import com.junsong.workflow.lowcode.domain.LcBizObject;
import java.util.List;

/**
 * NATIVE 存储模式：自动根据业务对象字段元数据生成独立物理表。
 *
 * @author Genesis·峻松
 */
public interface LcNativeTableGenerator
{
    /**
     * 为指定业务对象生成/更新独立物理表。
     *
     * @param bizObject 业务对象定义
     * @param fields    字段元数据列表
     */
    void generateOrUpdateTable(LcBizObject bizObject, List<LcBizField> fields);
}
