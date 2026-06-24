package com.junsong.system.controller;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.junsong.common.core.domain.R;
import com.junsong.common.core.text.Convert;
import com.junsong.common.core.utils.DateUtils;
import com.junsong.common.core.utils.StringUtils;
import com.junsong.common.core.utils.poi.ExcelUtil;
import com.junsong.common.core.web.controller.BaseController;
import com.junsong.common.core.web.domain.AjaxResult;
import com.junsong.common.core.web.page.TableDataInfo;
import com.junsong.common.log.annotation.Log;
import com.junsong.common.log.enums.BusinessType;
import com.junsong.common.security.annotation.InnerAuth;
import com.junsong.common.security.annotation.RequiresPermissions;
import com.junsong.common.security.service.TokenService;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.system.api.domain.SysDept;
import com.junsong.system.api.domain.SysRole;
import com.junsong.system.api.domain.SysUser;
import com.junsong.system.api.model.LoginUser;
import com.junsong.system.domain.SysUserDept;
import com.junsong.system.domain.SysInviteCode;
import com.junsong.system.mapper.SysInviteCodeMapper;
import com.junsong.system.service.ISysConfigService;
import com.junsong.system.service.ISysDeptService;
import com.junsong.system.service.ISysPermissionService;
import com.junsong.system.service.ISysPostService;
import com.junsong.system.service.ISysRoleService;
import com.junsong.system.service.ISysUserDeptService;
import com.junsong.system.service.ISysUserService;

/**
 * 用户信息
 * 
 * @author junsong
 */
@RestController
@RequestMapping("/user")
public class SysUserController extends BaseController
{
    @Autowired
    private ISysUserService userService;

    @Autowired
    private ISysRoleService roleService;

    @Autowired
    private ISysDeptService deptService;

    @Autowired
    private ISysPostService postService;

    @Autowired
    private ISysPermissionService permissionService;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private ISysUserDeptService userDeptService;

    @Autowired
    private SysInviteCodeMapper inviteCodeMapper;

    /**
     * 获取用户列表
     */
    @RequiresPermissions("system:user:list")
    @GetMapping("/list")
    public TableDataInfo list(SysUser user)
    {
        startPage();
        List<SysUser> list = userService.selectUserList(user);
        return getDataTable(list);
    }

    @Log(title = "用户管理", businessType = BusinessType.EXPORT)
    @RequiresPermissions("system:user:export")
    @PostMapping("/export")
    public void export(HttpServletResponse response, SysUser user)
    {
        List<SysUser> list = userService.selectUserList(user);
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        util.exportExcel(response, list, "用户数据");
    }

    @Log(title = "用户管理", businessType = BusinessType.IMPORT)
    @RequiresPermissions("system:user:import")
    @PostMapping("/importData")
    public AjaxResult importData(MultipartFile file, boolean updateSupport) throws Exception
    {
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        List<SysUser> userList = util.importExcel(file.getInputStream());
        String operName = SecurityUtils.getUsername();
        String message = userService.importUser(userList, updateSupport, operName);
        return success(message);
    }

    @PostMapping("/importTemplate")
    public void importTemplate(HttpServletResponse response) throws IOException
    {
        ExcelUtil<SysUser> util = new ExcelUtil<SysUser>(SysUser.class);
        util.importTemplateExcel(response, "用户数据");
    }

    /**
     * 获取当前用户信息
     */
    @InnerAuth
    @GetMapping("/info/{username}")
    public R<LoginUser> info(@PathVariable("username") String username)
    {
        SysUser sysUser = userService.selectUserByUserName(username);
        // 如果按用户名未找到，尝试按手机号查询
        if (StringUtils.isNull(sysUser) && username.matches("^1[3-9]\\d{9}$"))
        {
            sysUser = userService.selectUserByPhonenumber(username);
        }
        if (StringUtils.isNull(sysUser))
        {
            return R.fail("用户名或密码错误");
        }
        // 角色集合
        Set<String> roles = permissionService.getRolePermission(sysUser);
        // 权限集合
        Set<String> permissions = permissionService.getMenuPermission(sysUser);
        LoginUser sysUserVo = new LoginUser();
        sysUserVo.setSysUser(sysUser);
        // 设置部门ID：优先使用sys_user表的dept_id，为空时从关联表取第一个部门
        Long deptId = sysUser.getDeptId();
        if (deptId == null) {
            java.util.List<com.junsong.system.domain.SysUserDept> userDepts = userDeptService.selectUserDeptByUserId(sysUser.getUserId());
            if (userDepts != null && !userDepts.isEmpty()) {
                deptId = userDepts.get(0).getDeptId();
            }
        }
        sysUserVo.setDeptId(deptId);
        // 同步更新内部SysUser对象的dept_id，确保DataScope能正确读取
        sysUser.setDeptId(deptId);
        sysUserVo.setRoles(roles);
        sysUserVo.setPermissions(permissions);
        return R.ok(sysUserVo);
    }

    /**
     * 注册用户信息
     */
    @InnerAuth
    @PostMapping("/register")
    public R<Boolean> register(@RequestBody SysUser sysUser)
    {
        String username = sysUser.getUserName();
        if (!("true".equals(configService.selectConfigByKey("sys.account.registerUser"))))
        {
            return R.fail("当前系统没有开启注册功能！");
        }
        if (!userService.checkUserNameUnique(sysUser))
        {
            return R.fail("保存用户'" + username + "'失败，注册账号已存在");
        }
        // 手机号唯一校验
        if (StringUtils.isNotEmpty(sysUser.getPhonenumber()))
        {
            SysUser phoneCheck = new SysUser();
            phoneCheck.setPhonenumber(sysUser.getPhonenumber());
            if (!userService.checkPhoneUnique(phoneCheck))
            {
                return R.fail("注册失败，手机号已存在");
            }
        }
        else
        {
            return R.fail("手机号不能为空");
        }
        // 身份证必填校验
        boolean idCardRequired = "true".equals(configService.selectConfigByKey("sys.account.registerIdCardRequired"));
        if (idCardRequired && StringUtils.isEmpty(sysUser.getIdCard()))
        {
            return R.fail("身份证号不能为空");
        }
        // 邀请码校验
        boolean inviteCodeRequired = "true".equals(configService.selectConfigByKey("sys.account.registerInviteCode"));
        SysInviteCode inviteRecord = null;
        if (inviteCodeRequired)
        {
            if (StringUtils.isEmpty(sysUser.getInviteCode()))
            {
                return R.fail("邀请码不能为空");
            }
            inviteRecord = inviteCodeMapper.selectByCode(sysUser.getInviteCode());
            if (inviteRecord == null)
            {
                return R.fail("邀请码无效或已被使用");
            }
        }
        // 新用户是否需要审核
        boolean needAudit = "true".equals(configService.selectConfigByKey("sys.account.registerNeedAudit"));
        if (needAudit)
        {
            sysUser.setStatus("1"); // 停用，待审核
        }
        else
        {
            sysUser.setStatus("0"); // 正常
        }
        // 使用事务方法保证用户注册与邀请码标记的原子性
        String inviteCode = inviteRecord != null ? sysUser.getInviteCode() : null;
        boolean result = userService.registerUserWithInviteCode(sysUser, inviteCode, inviteCodeMapper);
        return R.ok(result);
    }

    /**
     * 生成邀请码（现有员工调用）
     */
    @PostMapping("/invite-code/generate")
    public R<String> generateInviteCode()
    {
        Long userId = SecurityUtils.getUserId();
        String username = SecurityUtils.getUsername();
        // 生成随机邀请码：8位大写字母+数字
        String code = generateRandomCode(8);
        SysInviteCode inviteCode = new SysInviteCode();
        inviteCode.setCode(code);
        inviteCode.setInviterUserId(userId);
        inviteCode.setInviterName(username);
        inviteCodeMapper.insertInviteCode(inviteCode);
        return R.ok(code);
    }

    /**
     * 生成随机邀请码（使用 SecureRandom 防止预测）
     */
    private String generateRandomCode(int length)
    {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        java.security.SecureRandom random = new java.security.SecureRandom();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++)
        {
            int index = random.nextInt(chars.length());
            sb.append(chars.charAt(index));
        }
        return sb.toString();
    }

    /**
     *记录用户登录IP地址和登录时间
     */
    @InnerAuth
    @PutMapping("/recordlogin")
    public R<Boolean> recordlogin(@RequestBody SysUser sysUser)
    {
        return R.ok(userService.updateLoginInfo(sysUser));
    }

    /**
     * 获取用户信息
     * 
     * @return 用户信息
     */
    @GetMapping("getInfo")
    public AjaxResult getInfo()
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = loginUser.getSysUser();
        // 角色集合
        Set<String> roles = permissionService.getRolePermission(user);
        // 权限集合
        Set<String> permissions = permissionService.getMenuPermission(user);
        if (!loginUser.getPermissions().equals(permissions))
        {
            loginUser.setPermissions(permissions);
            tokenService.refreshToken(loginUser);
        }
        
        // 获取用户可用的店面列表（在职状态）
        SysUserDept query = new SysUserDept();
        query.setUserId(user.getUserId());
        query.setStatus("0"); // 只查询在职的
        List<SysUserDept> userDeptList = userDeptService.selectSysUserDeptList(query);
        
        // 批量获取店面详细信息，避免 N+1 查询
        List<Long> deptIds = userDeptList.stream()
            .map(SysUserDept::getDeptId)
            .filter(deptId -> deptId != null)
            .distinct()
            .collect(Collectors.toList());
        List<SysDept> depts;
        if (deptIds.isEmpty())
        {
            depts = new ArrayList<>();
        }
        else
        {
            List<SysDept> deptList = deptService.selectDeptByIds(deptIds);
            Map<Long, SysDept> deptMap = deptList.stream()
                .collect(Collectors.toMap(SysDept::getDeptId, d -> d, (a, b) -> a));
            depts = userDeptList.stream()
                .map(ud -> deptMap.get(ud.getDeptId()))
                .filter(d -> d != null)
                .collect(Collectors.toList());
        }

        AjaxResult ajax = AjaxResult.success();
        ajax.put("user", user);
        ajax.put("roles", roles);
        ajax.put("permissions", permissions);
        ajax.put("pwdChrtype", getSysAccountChrtype());
        ajax.put("isDefaultModifyPwd", initPasswordIsModify(user.getPwdUpdateDate()));
        ajax.put("isPasswordExpired", passwordIsExpiration(user.getPwdUpdateDate()));
        ajax.put("depts", depts); // 用户可用的店面列表
        ajax.put("currentDeptId", loginUser.getDeptId()); // 当前选择的店面
        return ajax;
    }
    
    /**
     * 切换店面
     * 
     * @param deptId 店面ID
     * @return 结果
     */
    @PostMapping("switchDept/{deptId}")
    public AjaxResult switchDept(@PathVariable Long deptId)
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = loginUser.getSysUser();
        
        // 验证用户是否有这个店面的权限
        SysUserDept query = new SysUserDept();
        query.setUserId(user.getUserId());
        query.setDeptId(deptId);
        query.setStatus("0");
        List<SysUserDept> userDeptList = userDeptService.selectSysUserDeptList(query);
        
        if (userDeptList.isEmpty() && !SecurityUtils.isAdmin(user.getUserId()))
        {
            return error("您没有该店面的权限或已离职");
        }
        
        SysDept dept = deptService.selectDeptById(deptId);
        if (dept == null)
        {
            return error("部门不存在");
        }
        
        // 更新登录用户的店面ID
        loginUser.setDeptId(deptId);
        user.setDeptId(deptId);
        tokenService.refreshToken(loginUser);
        
        return success("切换成功");
    }

    // 获取用户密码自定义配置规则
    public String getSysAccountChrtype()
    {
        return Convert.toStr(configService.selectConfigByKey("sys.account.chrtype"), "0");
    }

    // 检查初始密码是否提醒修改
    public boolean initPasswordIsModify(Date pwdUpdateDate)
    {
        Integer initPasswordModify = Convert.toInt(configService.selectConfigByKey("sys.account.initPasswordModify"));
        return initPasswordModify != null && initPasswordModify == 1 && pwdUpdateDate == null;
    }

    // 检查密码是否过期
    public boolean passwordIsExpiration(Date pwdUpdateDate)
    {
        Integer passwordValidateDays = Convert.toInt(configService.selectConfigByKey("sys.account.passwordValidateDays"));
        if (passwordValidateDays != null && passwordValidateDays > 0)
        {
            if (StringUtils.isNull(pwdUpdateDate))
            {
                // 如果从未修改过初始密码，直接提醒过期
                return true;
            }
            Date nowDate = DateUtils.getNowDate();
            return DateUtils.differentDaysByMillisecond(nowDate, pwdUpdateDate) > passwordValidateDays;
        }
        return false;
    }

    /**
     * 根据用户编号获取详细信息
     */
    @RequiresPermissions("system:user:query")
    @GetMapping(value = { "/", "/{userId}" })
    public AjaxResult getInfo(@PathVariable(value = "userId", required = false) Long userId)
    {
        AjaxResult ajax = AjaxResult.success();
        if (StringUtils.isNotNull(userId))
        {
            userService.checkUserDataScope(userId);
            SysUser sysUser = userService.selectUserById(userId);
            ajax.put(AjaxResult.DATA_TAG, sysUser);
            ajax.put("postIds", postService.selectPostListByUserId(userId));
            ajax.put("roleIds", sysUser.getRoles().stream().map(SysRole::getRoleId).collect(Collectors.toList()));
        }
        List<SysRole> roles = roleService.selectRoleAll();
        ajax.put("roles", SecurityUtils.isAdmin(userId) ? roles : roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()));
        ajax.put("posts", postService.selectPostAll());
        return ajax;
    }

    /**
     * 新增用户
     */
    @RequiresPermissions("system:user:add")
    @Log(title = "用户管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody SysUser user)
    {
        deptService.checkDeptDataScope(user.getDeptId());
        roleService.checkRoleDataScope(user.getRoleIds());
        if (!userService.checkUserNameUnique(user))
        {
            return error("新增用户'" + user.getUserName() + "'失败，登录账号已存在");
        }
        else if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user))
        {
            return error("新增用户'" + user.getUserName() + "'失败，手机号码已存在");
        }
        else if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user))
        {
            return error("新增用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        user.setCreateBy(SecurityUtils.getUsername());
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        return toAjax(userService.insertUser(user));
    }

    /**
     * 修改用户
     */
    @RequiresPermissions("system:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@Validated @RequestBody SysUser user)
    {
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        deptService.checkDeptDataScope(user.getDeptId());
        roleService.checkRoleDataScope(user.getRoleIds());
        if (!userService.checkUserNameUnique(user))
        {
            return error("修改用户'" + user.getUserName() + "'失败，登录账号已存在");
        }
        else if (StringUtils.isNotEmpty(user.getPhonenumber()) && !userService.checkPhoneUnique(user))
        {
            return error("修改用户'" + user.getUserName() + "'失败，手机号码已存在");
        }
        else if (StringUtils.isNotEmpty(user.getEmail()) && !userService.checkEmailUnique(user))
        {
            return error("修改用户'" + user.getUserName() + "'失败，邮箱账号已存在");
        }
        user.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(userService.updateUser(user));
    }

    /**
     * 删除用户
     */
    @RequiresPermissions("system:user:remove")
    @Log(title = "用户管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{userIds}")
    public AjaxResult remove(@PathVariable Long[] userIds)
    {
        if (ArrayUtils.contains(userIds, SecurityUtils.getUserId()))
        {
            return error("当前用户不能删除");
        }
        return toAjax(userService.deleteUserByIds(userIds));
    }

    /**
     * 重置密码
     */
    @RequiresPermissions("system:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/resetPwd")
    public AjaxResult resetPwd(@RequestBody SysUser user)
    {
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        user.setPassword(SecurityUtils.encryptPassword(user.getPassword()));
        user.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(userService.resetPwd(user));
    }

    /**
     * 状态修改
     */
    @RequiresPermissions("system:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.UPDATE)
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@RequestBody SysUser user)
    {
        userService.checkUserAllowed(user);
        userService.checkUserDataScope(user.getUserId());
        user.setUpdateBy(SecurityUtils.getUsername());
        return toAjax(userService.updateUserStatus(user));
    }

    /**
     * 根据用户编号获取授权角色
     */
    @RequiresPermissions("system:user:query")
    @GetMapping("/authRole/{userId}")
    public AjaxResult authRole(@PathVariable("userId") Long userId)
    {
        AjaxResult ajax = AjaxResult.success();
        SysUser user = userService.selectUserById(userId);
        List<SysRole> roles = roleService.selectRolesByUserId(userId);
        ajax.put("user", user);
        ajax.put("roles", SecurityUtils.isAdmin(userId) ? roles : roles.stream().filter(r -> !r.isAdmin()).collect(Collectors.toList()));
        return ajax;
    }

    /**
     * 用户授权角色
     */
    @RequiresPermissions("system:user:edit")
    @Log(title = "用户管理", businessType = BusinessType.GRANT)
    @PutMapping("/authRole")
    public AjaxResult insertAuthRole(Long userId, Long[] roleIds)
    {
        userService.checkUserDataScope(userId);
        roleService.checkRoleDataScope(roleIds);
        userService.insertUserAuth(userId, roleIds);
        return success();
    }

    /**
     * 获取部门树列表
     */
    @RequiresPermissions("system:user:list")
    @GetMapping("/deptTree")
    public AjaxResult deptTree(SysDept dept)
    {
        return success(deptService.selectDeptTreeList(dept));
    }

    /**
     * 登录前根据用户名获取可选部门（内部接口，由 auth 模块通过 Feign 调用）
     */
    @InnerAuth
    @GetMapping("/deptsForLogin")
    public R<List<SysDept>> deptsForLogin(@RequestParam("username") String username)
    {
        SysUser sysUser = userService.selectUserByUserName(username);
        if (StringUtils.isNull(sysUser))
        {
            return R.fail("用户不存在");
        }
        return getUserDeptsByUserId(sysUser.getUserId());
    }

    /**
     * 根据用户名获取用户部门列表（供远程调用）
     */
    @InnerAuth
    @GetMapping("/dept/list")
    public R<List<SysDept>> getUserDeptList(@RequestParam("username") String username)
    {
        SysUser sysUser = userService.selectUserByUserName(username);
        if (StringUtils.isNull(sysUser))
        {
            return R.fail("用户不存在");
        }
        return getUserDeptsByUserId(sysUser.getUserId());
    }

    /**
     * 根据用户ID获取用户部门列表（供远程调用）
     */
    @InnerAuth
    @GetMapping("/dept/listByUserId")
    public R<List<SysDept>> getUserDeptsByUserId(@RequestParam("userId") Long userId)
    {
        // 获取用户可用的店面列表（在职状态）
        SysUserDept query = new SysUserDept();
        query.setUserId(userId);
        query.setStatus("0"); // 只查询在职的
        List<SysUserDept> userDeptList = userDeptService.selectSysUserDeptList(query);
        
        // 批量获取店面详细信息，避免 N+1 查询
        List<Long> deptIds = userDeptList.stream()
            .map(SysUserDept::getDeptId)
            .filter(deptId -> deptId != null)
            .distinct()
            .collect(Collectors.toList());
        List<SysDept> depts;
        if (deptIds.isEmpty())
        {
            depts = new ArrayList<>();
        }
        else
        {
            List<SysDept> deptList = deptService.selectDeptByIds(deptIds);
            Map<Long, SysDept> deptMap = deptList.stream()
                .collect(Collectors.toMap(SysDept::getDeptId, d -> d, (a, b) -> a));
            depts = userDeptList.stream()
                .map(ud -> deptMap.get(ud.getDeptId()))
                .filter(d -> d != null)
                .collect(Collectors.toList());
        }

        return R.ok(depts);
    }

    /**
     * 按角色 key 查询该角色下所有用户的 username 列表（供工作流远程调用）
     */
    @InnerAuth
    @GetMapping("/list-by-role")
    public R<List<String>> listUsernamesByRoleKey(@RequestParam("roleKey") String roleKey)
    {
        List<String> usernames = userService.selectUsernameListByRoleKey(roleKey);
        return R.ok(usernames);
    }
}
