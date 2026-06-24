package com.junsong.workflow.lowcode.validator;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.system.api.RemoteDeptService;
import com.junsong.system.api.domain.SysDept;
import com.junsong.workflow.lowcode.domain.LcBizObject;

/**
 * 空间查重校验器（门店选址场景）。
 *
 * <p>校验逻辑：
 * <ol>
 *   <li>从 formData 中按 {@code geoField} 取候选坐标（支持 "lng,lat" 字符串或 {lng,lat} 对象）</li>
 *   <li>以 radiusKm 为半径算包围盒（Δ = radiusKm/111），调用 RemoteDeptService 预筛候选门店</li>
 *   <li>对预筛结果用 Haversine 公式精算球面距离</li>
 *   <li>若最近距离 &lt; radiusKm，抛 ServiceException 阻断提交</li>
 * </ol>
 *
 * <p>配置示例：
 * <pre>
 * {
 *   "type": "GEO_DEDUP",
 *   "geoField": "store_location",
 *   "radiusKm": 1.5,
 *   "message": "周边1.5公里内已存在门店「${nearestName}」(距离${distance}米)，不可重复开店"
 * }
 * </pre>
 *
 * @author junsong
 */
@Component
public class GeoDedupValidator implements LcSubmitValidator
{
    private static final Logger log = LoggerFactory.getLogger(GeoDedupValidator.class);

    /** 地球半径（km），用于 Haversine 公式 */
    private static final double EARTH_RADIUS_KM = 6371.0;

    /** 1 度纬度约等于 111 km，用于包围盒预筛 */
    private static final double KM_PER_DEGREE = 111.0;

    @Autowired
    private RemoteDeptService remoteDeptService;

    @Override
    public String type()
    {
        return "GEO_DEDUP";
    }

    @Override
    public void validate(LcBizObject bizObject, Map<String, Object> formData, Map<String, Object> config)
            throws ServiceException
    {
        // 1. 读取配置
        String geoField = asString(config.get("geoField"), "store_location");
        double radiusKm = asDouble(config.get("radiusKm"), 1.5);
        String messageTemplate = asString(config.get("message"),
                "周边" + radiusKm + "公里内已存在门店「${nearestName}」(距离${distance}米)，不可重复开店");

        // 2. 从 formData 取坐标
        double[] lngLat = extractLngLat(formData, geoField);
        if (lngLat == null)
        {
            throw new ServiceException("空间查重失败：表单字段 [" + geoField + "] 未提供有效坐标");
        }
        double lng = lngLat[0];
        double lat = lngLat[1];

        // 3. 包围盒预筛（Δ = radiusKm / 111）
        double delta = radiusKm / KM_PER_DEGREE;
        double minLng = lng - delta;
        double maxLng = lng + delta;
        double minLat = lat - delta;
        double maxLat = lat + delta;

        R<List<SysDept>> r;
        try
        {
            r = remoteDeptService.listStoreGeo(minLng, maxLng, minLat, maxLat,
                    SecurityConstants.FROM_SOURCE);
        }
        catch (Exception e)
        {
            log.error("空间查重：调用门店坐标服务失败 bizCode={}", bizObject.getBizCode(), e);
            // 降级策略：查重服务不可用时，为避免误阻断业务，放行但告警
            log.warn("空间查重服务不可用，本次提交放行（降级）");
            return;
        }
        if (r == null || r.getData() == null || r.getData().isEmpty())
        {
            // 包围盒内无候选门店，查重通过
            return;
        }

        // 4. Haversine 精算，找最近门店
        String nearestName = null;
        double nearestDistanceKm = Double.MAX_VALUE;
        for (SysDept dept : r.getData())
        {
            BigDecimal deptLng = dept.getLongitude();
            BigDecimal deptLat = dept.getLatitude();
            if (deptLng == null || deptLat == null)
            {
                continue;
            }
            double d = haversineKm(lat, lng, deptLat.doubleValue(), deptLng.doubleValue());
            if (d < nearestDistanceKm)
            {
                nearestDistanceKm = d;
                nearestName = dept.getDeptName();
            }
        }

        // 5. 最近距离 < radiusKm 则阻断
        if (nearestDistanceKm < radiusKm)
        {
            long distanceMeters = Math.round(nearestDistanceKm * 1000);
            String msg = renderMessage(messageTemplate, nearestName, distanceMeters);
            throw new ServiceException(msg);
        }
    }

    /**
     * 从 formData 中提取经纬度。支持两种格式：
     * <ul>
     *   <li>字符串："lng,lat"（如 "116.404,39.915"）</li>
     *   <li>对象：{"lng": ..., "lat": ...} 或 {"longitude":..., "latitude":...}</li>
     * </ul>
     *
     * @return double[]{lng, lat}，无法解析返回 null
     */
    @SuppressWarnings("unchecked")
    private double[] extractLngLat(Map<String, Object> formData, String geoField)
    {
        if (formData == null)
        {
            return null;
        }
        Object val = formData.get(geoField);
        if (val == null)
        {
            return null;
        }
        // 字符串 "lng,lat"
        if (val instanceof String s)
        {
            String[] parts = s.split("[,，]");
            if (parts.length == 2)
            {
                try
                {
                    return new double[] { Double.parseDouble(parts[0].trim()), Double.parseDouble(parts[1].trim()) };
                }
                catch (NumberFormatException ignore)
                {
                    return null;
                }
            }
            return null;
        }
        // 对象 {lng, lat} 或 {longitude, latitude}
        if (val instanceof Map<?, ?> m)
        {
            Object lngObj = m.get("lng");
            if (lngObj == null) lngObj = m.get("longitude");
            Object latObj = m.get("lat");
            if (latObj == null) latObj = m.get("latitude");
            if (lngObj != null && latObj != null)
            {
                try
                {
                    return new double[] { Double.parseDouble(lngObj.toString()), Double.parseDouble(latObj.toString()) };
                }
                catch (NumberFormatException ignore)
                {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * Haversine 公式计算两点球面距离（km）。
     *
     * @param lat1 起点纬度
     * @param lng1 起点经度
     * @param lat2 终点纬度
     * @param lng2 终点经度
     * @return 距离（km）
     */
    private double haversineKm(double lat1, double lng1, double lat2, double lng2)
    {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                  * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS_KM * c;
    }

    /**
     * 渲染消息模板，替换占位符 ${nearestName} 和 ${distance}。
     */
    private String renderMessage(String template, String nearestName, long distanceMeters)
    {
        String name = StringUtils.isNotEmpty(nearestName) ? nearestName : "未知门店";
        return template.replace("${nearestName}", name).replace("${distance}", String.valueOf(distanceMeters));
    }

    private String asString(Object obj, String defaultVal)
    {
        return obj == null ? defaultVal : obj.toString();
    }

    private double asDouble(Object obj, double defaultVal)
    {
        if (obj == null) return defaultVal;
        if (obj instanceof Number n) return n.doubleValue();
        try
        {
            return Double.parseDouble(obj.toString());
        }
        catch (NumberFormatException e)
        {
            return defaultVal;
        }
    }
}
