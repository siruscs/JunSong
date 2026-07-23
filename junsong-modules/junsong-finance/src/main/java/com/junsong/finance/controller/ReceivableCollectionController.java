package com.junsong.finance.controller;

import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.vo.ReceivableCollectionSyncParams;
import com.junsong.finance.domain.vo.ReceivableCollectionUpdateParams;
import com.junsong.finance.service.IReceivableCollectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ReceivableCollectionController extends BaseController {

    @Autowired
    private IReceivableCollectionService receivableCollectionService;

    private ReceivableCollectionSyncParams scoped(ReceivableCollectionSyncParams params) {
        ReceivableCollectionSyncParams result = params == null ? new ReceivableCollectionSyncParams() : params;
        if (!SecurityUtils.isAdmin()) {
            Long deptId = SecurityUtils.getDeptId();
            result.setDeptId(deptId);
            result.setDeptIds(deptId == null
                    ? java.util.Collections.singletonList(-1L)
                    : java.util.Collections.singletonList(deptId));
        }
        return result;
    }

    @RequiresPermissions("finance:receivableCollection:list")
    @PostMapping("/receivable-collection/dashboard")
    public AjaxResult dashboard(@RequestBody(required = false) ReceivableCollectionSyncParams params) {
        return AjaxResult.success(receivableCollectionService.getDashboard(scoped(params)));
    }

    @RequiresPermissions("finance:receivableCollection:list")
    @PostMapping("/receivable-collection/list")
    public TableDataInfo list(@RequestBody(required = false) ReceivableCollectionSyncParams params) {
        startPage();
        return getDataTable(receivableCollectionService.list(scoped(params)));
    }

    @RequiresPermissions("finance:receivableCollection:sync")
    @PostMapping("/receivable-collection/sync")
    public AjaxResult sync(@RequestBody(required = false) ReceivableCollectionSyncParams params) {
        int count = receivableCollectionService.syncFromReceivables(scoped(params));
        return AjaxResult.success("成功同步 " + count + " 条应收催收记录", count);
    }

    @RequiresPermissions("finance:receivableCollection:edit")
    @PostMapping("/receivable-collection/{collectionId}/follow")
    public AjaxResult follow(@PathVariable Long collectionId, @RequestBody ReceivableCollectionUpdateParams params) {
        if (!receivableCollectionService.canAccess(collectionId, SecurityUtils.getDeptId())) {
            return AjaxResult.error("催收记录不存在或无权操作");
        }
        receivableCollectionService.updateFollow(collectionId, params);
        return AjaxResult.success("催收跟进已保存");
    }
}
