package com.junsong.system.api;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.constant.ServiceNameConstants;
import com.junsong.common.core.domain.R;
import com.junsong.system.api.domain.SysDept;
import com.junsong.system.api.domain.SysUser;
import com.junsong.system.api.factory.RemoteUserFallbackFactory;
import com.junsong.system.api.model.LoginUser;
import java.util.List;

/**
 * 用户服务
 * 
 * @author ruoyi
 */
@FeignClient(contextId = "remoteUserService", value = ServiceNameConstants.SYSTEM_SERVICE, fallbackFactory = RemoteUserFallbackFactory.class)
public interface RemoteUserService
{
    /**
     * 通过用户名查询用户信息
     *
     * @param username 用户名
     * @param source 请求来源
     * @return 结果
     */
    @GetMapping("/user/info/{username}")
    public R<LoginUser> getUserInfo(@PathVariable("username") String username, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 注册用户信息
     *
     * @param sysUser 用户信息
     * @param source 请求来源
     * @return 结果
     */
    @PostMapping("/user/register")
    public R<Boolean> registerUserInfo(@RequestBody SysUser sysUser, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 记录用户登录IP地址和登录时间
     *
     * @param sysUser 用户信息
     * @param source 请求来源
     * @return 结果
     */
    @PutMapping("/user/recordlogin")
    public R<Boolean> recordUserLogin(@RequestBody SysUser sysUser, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 通过用户名查询用户授权门店列表
     *
     * @param username 用户名
     * @param source 请求来源
     * @return 用户授权门店列表
     */
    @GetMapping("/user/dept/list")
    public R<List<SysDept>> getUserDeptList(@RequestParam("username") String username, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);

    /**
     * 按角色 key 查询该角色下的所有用户名
     * 用于工作流 candidateGroups 解析（如 candidateGroups="hr" → 返回 hr 组下所有用户名）
     *
     * @param roleKey 角色标识（如 "hr"、"common"）
     * @param source 请求来源
     * @return 用户名列表
     */
    @GetMapping("/user/list-by-role")
    public R<List<String>> listUsernamesByRoleKey(@RequestParam("roleKey") String roleKey, @RequestHeader(SecurityConstants.FROM_SOURCE) String source);
}
