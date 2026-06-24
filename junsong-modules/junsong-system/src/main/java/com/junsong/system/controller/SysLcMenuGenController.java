package com.junsong.system.controller;

import com.junsong.common.core.domain.R;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.security.annotation.InnerAuth;
import com.junsong.system.api.domain.LcMenuGenerateRequest;
import com.junsong.system.service.ISysLcMenuGenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 低代码菜单自动生成（INNER 内部接口）
 */
@RestController
@RequestMapping("/menu/inner")
public class SysLcMenuGenController extends BaseController
{
    @Autowired
    private ISysLcMenuGenService lcMenuGenService;

    @InnerAuth
    @PostMapping("/lowcode-generate")
    public R<Long> generate(@RequestBody LcMenuGenerateRequest request)
    {
        return R.ok(lcMenuGenService.generate(request));
    }
}
