package com.junsong.system.api;

import java.util.List;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.constant.ServiceNameConstants;
import com.junsong.common.core.domain.R;
import com.junsong.system.api.domain.SysDept;
import com.junsong.system.api.factory.RemoteDeptFallbackFactory;

/**
 * 部门服务（供工作流模块远程调用）
 */
@FeignClient(contextId = "remoteDeptService", value = ServiceNameConstants.SYSTEM_SERVICE, fallbackFactory = RemoteDeptFallbackFactory.class)
public interface RemoteDeptService
{
    /**
     * 查询包围盒内的门店坐标列表（供空间查重使用）
     *
     * @param minLng 最小经度
     * @param maxLng 最大经度
     * @param minLat 最小纬度
     * @param maxLat 最大纬度
     * @param source 请求来源
     * @return 门店列表
     */
    @GetMapping("/dept/store-geo")
    R<List<SysDept>> listStoreGeo(
            @RequestParam("minLng") double minLng,
            @RequestParam("maxLng") double maxLng,
            @RequestParam("minLat") double minLat,
            @RequestParam("maxLat") double maxLat,
            @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
