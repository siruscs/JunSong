package com.junsong.workflow.service.identity;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import com.junsong.system.api.domain.SysUser;
import com.junsong.system.api.model.LoginUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * 工作流身份适配层（PeerUserService）
 *
 * 职责：
 * - 把峻松主体的 sys_user / sys_dept 体系映射到 Flowable 的 User / Group 概念
 * - 封装"按 username 拿 LoginUser / 拿用户授权部门列表"等基础能力
 * - 为 Flowable BPMN 表达式提供一组稳定的 SpEL 友好方法
 *
 * 设计约定：
 * - Flowable assignee / candidateUsers 一律使用 username 字符串作为身份
 * - Flowable candidateGroups 使用角色 roleKey（例如 "hr"、"finance"）
 * - 后续如需扩展（按 deptId 反查 user 等）由 system 服务扩展 Feign 接口
 *
 * @author junsong
 */
@Service("peerUserService")
public class PeerUserService {

    private static final Logger log = LoggerFactory.getLogger(PeerUserService.class);

    @Autowired
    private RemoteUserService remoteUserService;

    /**
     * 获取并校验正常状态的用户。
     */
    public SysUser requireActiveUser(String username) {
        if (username == null || username.isBlank()) {
            throw new ServiceException("用户名不能为空");
        }
        String normalizedUsername = username.trim();
        R<LoginUser> result;
        try {
            result = remoteUserService.getUserInfo(normalizedUsername, SecurityConstants.INNER);
        } catch (Exception e) {
            throw new ServiceException("用户体系服务不可用: " + e.getMessage());
        }
        if (result == null) {
            throw new ServiceException("用户体系服务不可用: 空响应");
        }
        if (R.isError(result)) {
            if ("用户名或密码错误".equals(result.getMsg()) || "用户不存在".equals(result.getMsg())) {
                throw new ServiceException("用户不存在: " + normalizedUsername);
            }
            throw new ServiceException("用户体系服务不可用: " + result.getMsg());
        }
        LoginUser loginUser = result.getData();
        SysUser user = loginUser == null ? null : loginUser.getSysUser();
        if (user == null) {
            throw new ServiceException("用户不存在: " + normalizedUsername);
        }
        String canonicalUsername = user.getUserName();
        if (canonicalUsername == null || canonicalUsername.isBlank()) {
            throw new ServiceException("用户体系返回的 username 为空: " + normalizedUsername);
        }
        if (!canonicalUsername.trim().equalsIgnoreCase(normalizedUsername)) {
            throw new ServiceException("用户体系返回的 username 不匹配: " + normalizedUsername);
        }
        if (!"0".equals(user.getStatus()) || !"0".equals(user.getDelFlag())) {
            throw new ServiceException("用户已停用或删除: " + normalizedUsername);
        }
        return user;
    }

    /**
     * 归一化并校验 Flowable 审批人 username。
     */
    public String requireUsername(Object raw) {
        String username = normalizeAssignee(raw);
        if (username == null) {
            throw new ServiceException("审批人必须使用有效 username");
        }
        return requireActiveUser(username).getUserName().trim();
    }

    /**
     * 获取用户授权部门；服务异常必须显式失败，避免工作流静默选错审批链。
     */
    public List<SysDept> requireAuthorizedDepts(String username) {
        requireActiveUser(username);
        return requireAuthorizedDeptsForValidatedUser(username);
    }

    /**
     * 获取已完成用户校验的 username 所对应的授权部门。
     */
    List<SysDept> requireAuthorizedDeptsForValidatedUser(String username) {
        String normalizedUsername = username.trim();
        R<List<SysDept>> result;
        try {
            result = remoteUserService.getUserDeptList(normalizedUsername, SecurityConstants.INNER);
        } catch (Exception e) {
            throw new ServiceException("用户体系服务不可用: " + e.getMessage());
        }
        if (result == null) {
            throw new ServiceException("用户体系服务不可用: 空响应");
        }
        if (R.isError(result)) {
            throw new ServiceException("用户体系服务不可用: " + result.getMsg());
        }
        List<SysDept> data = result.getData();
        if (data != null && data.stream().anyMatch(java.util.Objects::isNull)) {
            throw new ServiceException("用户体系服务不可用: 授权部门数据包含空值");
        }
        return data == null || data.isEmpty() ? List.of() : List.copyOf(data);
    }

    /**
     * 通过 username 获取 SysUser
     */
    public Optional<SysUser> getSysUserByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        try {
            R<LoginUser> result = remoteUserService.getUserInfo(username, SecurityConstants.INNER);
            if (R.isError(result)) {
                return Optional.empty();
            }
            LoginUser loginUser = result.getData();
            if (loginUser == null) {
                return Optional.empty();
            }
            return Optional.ofNullable(loginUser.getSysUser());
        } catch (Exception e) {
            log.warn("[Workflow] 通过 username={} 查 SysUser 失败：{}", username, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * 获取某用户被授权的部门 / 门店列表
     */
    public List<SysDept> listAuthorizedDepts(String username) {
        if (username == null || username.isBlank()) {
            return Collections.emptyList();
        }
        try {
            R<List<SysDept>> result = remoteUserService.getUserDeptList(username, SecurityConstants.INNER);
            if (R.isError(result)) {
                return Collections.emptyList();
            }
            List<SysDept> data = result.getData();
            return data == null ? Collections.emptyList() : data;
        } catch (Exception e) {
            log.warn("[Workflow] 通过 username={} 查授权部门失败：{}", username, e.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * 把姓名 / username / userId 形式的输入归一化为 Flowable assignee（统一为 username）
     * - 已经是 username（非数字）：原样返回
     * - 数字 userId：当前体系下需要扩展 RemoteUserService.getUserById 才能解析，先返回 null
     */
    public String normalizeAssignee(Object raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.toString().trim();
        if (s.isEmpty()) {
            return null;
        }
        if (s.matches("\\d+")) {
            log.debug("[Workflow] 输入是数字 userId={}，当前不支持反查 username，请使用 username 作为 assignee", s);
            return null;
        }
        return s;
    }

    /**
     * 按角色 key 查询该角色下所有正常用户的 username 列表
     * 用于工作流 candidateGroups 解析（如 BPMN 中 candidateGroups="hr" → 返回 ["zhaoliu", ...]）
     */
    public List<String> listUsernamesByRoleKey(String roleKey) {
        if (roleKey == null || roleKey.isBlank()) {
            return Collections.emptyList();
        }
        try {
            R<List<String>> result = remoteUserService.listUsernamesByRoleKey(roleKey, SecurityConstants.INNER);
            if (R.isError(result)) {
                log.warn("[Workflow] 按角色 roleKey={} 查用户失败: {}", roleKey, result.getMsg());
                return Collections.emptyList();
            }
            List<String> data = result.getData();
            return data == null ? Collections.emptyList() : data;
        } catch (Exception e) {
            log.warn("[Workflow] 按角色 roleKey={} 查用户异常: {}", roleKey, e.getMessage());
            return Collections.emptyList();
        }
    }
}
