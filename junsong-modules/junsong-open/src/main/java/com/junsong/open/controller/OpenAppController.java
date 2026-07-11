package com.junsong.open.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.alibaba.fastjson2.JSON;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.utils.SensitiveDataMasker;
import com.junsong.common.core.utils.poi.ExcelUtil;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.open.domain.OpenApp;
import com.junsong.open.domain.OpenAppSecret;
import com.junsong.open.service.IOpenAppSecretService;
import com.junsong.open.service.IOpenAppService;
import com.junsong.system.api.RemoteOperationAuditService;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 开放平台应用管理Controller
 *
 * @author junsong
 */
@RestController
@RequestMapping("/app")
public class OpenAppController extends BaseController
{
    private static final Logger log = LoggerFactory.getLogger(OpenAppController.class);

    @Autowired
    private IOpenAppService openAppService;

    @Autowired
    private IOpenAppSecretService openAppSecretService;

    @Autowired
    private RemoteOperationAuditService auditService;

    @GetMapping("/list")
    @RequiresPermissions("open:app:list")
    public TableDataInfo list(OpenApp openApp)
    {
        startPage();
        List<OpenApp> list = openAppService.selectOpenAppList(openApp);
        return getDataTable(list);
    }

    @PostMapping("/export")
    @RequiresPermissions("open:app:export")
    public void export(HttpServletResponse response, OpenApp openApp)
    {
        List<OpenApp> list = openAppService.selectOpenAppList(openApp);
        ExcelUtil<OpenApp> util = new ExcelUtil<>(OpenApp.class);
        util.exportExcel(response, list, "开放平台应用数据");
    }

    @GetMapping("/{id}")
    @RequiresPermissions("open:app:query")
    public AjaxResult getInfo(@PathVariable("id") Long id)
    {
        return success(openAppService.selectOpenAppById(id));
    }

    @PostMapping
    @RequiresPermissions("open:app:add")
    public AjaxResult add(@RequestBody OpenApp openApp)
    {
        openApp.setCreateBy(SecurityUtils.getUsername());
        return toAjax(openAppService.insertOpenApp(openApp));
    }

    @PutMapping
    @RequiresPermissions("open:app:edit")
    public AjaxResult edit(@RequestBody OpenApp openApp)
    {
        openApp.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(openAppService.updateOpenApp(openApp));
    }

    @DeleteMapping("/{ids}")
    @RequiresPermissions("open:app:remove")
    public AjaxResult remove(@PathVariable("ids") Long[] ids)
    {
        return toAjax(openAppService.deleteOpenAppByIds(ids));
    }

    @PutMapping("/approve/{appId}")
    @RequiresPermissions("open:app:approve")
    public AjaxResult approve(@PathVariable("appId") Long appId)
    {
        return toAjax(openAppService.approveApp(appId, SecurityUtils.getUsername()));
    }

    @PutMapping("/reject/{appId}")
    @RequiresPermissions("open:app:approve")
    public AjaxResult reject(@PathVariable("appId") Long appId, @RequestParam String rejectReason)
    {
        return toAjax(openAppService.rejectApp(appId, rejectReason, SecurityUtils.getUsername()));
    }

    @GetMapping("/keys/{appId}")
    @RequiresPermissions("open:app:key:list")
    public AjaxResult listKeys(@PathVariable("appId") Long appId)
    {
        List<OpenAppSecret> keys = openAppSecretService.selectKeysByAppId(appId);
        maskSecrets(keys);
        return success(keys);
    }

    @GetMapping("/keys/list")
    @RequiresPermissions("open:app:key:list")
    public TableDataInfo listAllKeys(OpenAppSecret openAppSecret)
    {
        startPage();
        List<OpenAppSecret> list = openAppSecretService.selectOpenAppSecretList(openAppSecret);
        maskSecrets(list);
        return getDataTable(list);
    }

    @PutMapping("/keys/changeStatus")
    @RequiresPermissions("open:app:key:edit")
    public AjaxResult changeKeyStatus(@RequestBody OpenAppSecret openAppSecret)
    {
        // R25 审计：捕获变更前快照
        OpenAppSecret before = null;
        try {
            before = openAppSecretService.selectKeysByAppId(openAppSecret.getAppId()).stream()
                    .filter(s -> s.getId().equals(openAppSecret.getId()))
                    .findFirst()
                    .orElse(null);
        } catch (Exception ex) {
            log.warn("R25 审计获取 before 失败 appId={}, id={}: {}", openAppSecret.getAppId(), openAppSecret.getId(), ex.getMessage());
        }

        int rows = openAppSecretService.changeStatus(openAppSecret);

        // R25 审计：捕获变更后快照并记录
        try {
            OpenAppSecret after = openAppSecretService.selectKeysByAppId(openAppSecret.getAppId()).stream()
                    .filter(s -> s.getId().equals(openAppSecret.getId()))
                    .findFirst()
                    .orElse(null);
            Map<String, Object> body = new HashMap<>();
            body.put("bizType", "OPEN_APP_SECRET");
            body.put("bizId", String.valueOf(openAppSecret.getId()));
            body.put("operation", "CHANGE_STATUS");
            body.put("riskLevel", "HIGH");
            body.put("beforeJson", before == null ? "" : JSON.toJSONString(before));
            body.put("afterJson", after == null ? "" : JSON.toJSONString(after));
            auditService.recordSnapshot(body, SecurityConstants.INNER);
        } catch (Exception ex) {
            log.warn("R25 审计记录失败 OPEN_APP_SECRET CHANGE_STATUS id={}: {}", openAppSecret.getId(), ex.getMessage());
        }
        return toAjax(rows);
    }

    /**
     * 控制台返回 appSecret 时脱敏：统一返回 ******。
     * SensitiveDataMasker.maskSensitive 覆盖以下字段：
     * - appSecret / token / password：完全掩码为 ******
     * - mobile / phone：保留前三位和后四位，例如 138****8000
     * - idCard / idNo：保留前六位和后四位，例如 110101********1234
     * - bankCard：保留后四位，例如 **** **** **** 1234
     * - email：保留首字母和域名，例如 a***@example.com
     * - webhookUrl / callbackUrl：保留协议和域名，不返回 path/query
     * - openId / unionId：完全掩码为 ******
     */
    private void maskSecrets(List<OpenAppSecret> keys)
    {
        if (keys == null)
        {
            return;
        }
        for (OpenAppSecret key : keys)
        {
            String appSecret = key.getAppSecret();
            if (appSecret != null)
            {
                // appSecret / token / webhookUrl / callbackUrl / idCard / mobile 等敏感字段统一脱敏
                key.setAppSecret(SensitiveDataMasker.maskSensitive(
                    "{\"appSecret\":\"" + appSecret + "\",\"webhookUrl\":\"" + appSecret + "\"}")
                    .replaceAll(".*\"appSecret\":\"([^\"]*)\".*", "$1"));
            }
        }
    }
}
