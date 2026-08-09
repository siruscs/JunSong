package com.junsong.member.service;

import java.util.List;
import com.junsong.member.domain.SysMpModuleSort;

/**
 * 小程序模块显示顺序配置服务。
 *
 * <p>提供「查询所有模块排序」和「整体重排保存」两个核心能力。
 * PC 端 mpPerm 页的【功能模块调整】弹窗调用 saveBatch 保存拖拽后顺序；
 * MpModuleCatalog / MemMpController 从 selectAll() 拿到排序值后覆盖默认 hardcode 顺序。
 */
public interface ISysMpModuleSortService {

    /** 查询所有排序配置。 */
    List<SysMpModuleSort> selectAll();

    /** 整体批量保存排序（先清空，再按入参顺序以 10 步进的 sort_order 重新插入）。 */
    void saveBatch(List<SysMpModuleSort> sortList);

    /** 单条保存（不存在 insert，存在 update）。一般用于后端补漏，前端整体保存请调用 saveBatch。 */
    int saveOne(SysMpModuleSort record);
}
