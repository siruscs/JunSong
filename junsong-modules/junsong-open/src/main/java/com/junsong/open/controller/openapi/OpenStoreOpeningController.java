package com.junsong.open.controller.openapi;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import com.junsong.common.core.idempotency.Idempotent;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;


/**
 * 门店选址即服务(Store Opening as a Service)开放API
 *
 * 对外暴露门店开店申请能力，配合工作流实现"选址申请→审批→开店"端到端流程
 *
 * @author junsong
 */
@RestController
@RequestMapping({"/store-openings", "/store-opening"})
public class OpenStoreOpeningController extends BaseController
{
    @Autowired
    private RestTemplate restTemplate;

    private static final String SYSTEM_BASE = "http://junsong-modules-system:9201";

    /**
     * 查询门店开店申请列表
     */
    @GetMapping
    public String listStoreOpenings(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String storeName,
            @RequestParam(required = false) String status)
    {
        String url = SYSTEM_BASE + "/storeOpening/list?pageNum=" + pageNum + "&pageSize=" + pageSize;
        if (storeName != null) url += "&storeName=" + storeName;
        if (status != null) url += "&status=" + status;
        return restTemplate.getForObject(url, String.class);
    }

    /**
     * 获取门店开店申请详情
     */
    @GetMapping("/{id}")
    public AjaxResult getStoreOpening(@PathVariable("id") Long id)
    {
        return restTemplate.getForObject(SYSTEM_BASE + "/storeOpening/" + id, AjaxResult.class);
    }

    /**
     * 按订单号查询门店开店申请
     */
    @GetMapping("/orders/{orderNo}")
    public AjaxResult getByOrderNo(@PathVariable("orderNo") String orderNo)
    {
        return restTemplate.getForObject(SYSTEM_BASE + "/storeOpening/order/" + orderNo, AjaxResult.class);
    }

    /**
     * 新增门店开店申请
     */
    @PostMapping
    @Idempotent(scene = "open:store-opening:add", highRisk = true)
    public AjaxResult addStoreOpening(@RequestBody Map<String, Object> params)
    {
        return restTemplate.postForObject(SYSTEM_BASE + "/storeOpening", params, AjaxResult.class);
    }

    /**
     * 提交审批
     */
    @PostMapping("/{id}/submit")
    @Idempotent(scene = "open:store-opening:submit", highRisk = true)
    public AjaxResult submitStoreOpening(@PathVariable("id") Long id, @RequestBody(required = false) Map<String, Object> params)
    {
        return restTemplate.postForObject(SYSTEM_BASE + "/storeOpening/" + id + "/submit", params, AjaxResult.class);
    }

    /**
     * 撤回申请
     */
    @PostMapping("/{id}/withdraw")
    @Idempotent(scene = "open:store-opening:withdraw", highRisk = true)
    public AjaxResult withdrawStoreOpening(@PathVariable("id") Long id, @RequestBody(required = false) Map<String, Object> params)
    {
        return restTemplate.postForObject(SYSTEM_BASE + "/storeOpening/" + id + "/withdraw", params, AjaxResult.class);
    }
}
