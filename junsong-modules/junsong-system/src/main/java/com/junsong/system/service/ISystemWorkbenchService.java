package com.junsong.system.service;

import com.junsong.system.domain.vo.WorkbenchTaskVO;
import java.util.List;

/**
 * 统一工作台任务聚合服务。
 *
 * @author junsong
 */
public interface ISystemWorkbenchService {

    /**
     * 聚合各来源的工作台任务。
     * 第一阶段聚合 SYSTEM 治理问题与 STOCK 库存健康问题；
     * FINANCE/MEMBER 跨模块来源在具备 Feign 集成后接入，当前不返回假数据。
     *
     * @return 工作台任务列表
     */
    List<WorkbenchTaskVO> aggregateTasks();
}