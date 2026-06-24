package com.junsong.workflow.service.identity;

import com.junsong.common.core.exception.ServiceException;
import com.junsong.system.api.domain.SysDept;
import com.junsong.system.api.domain.SysUser;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 组织架构解析服务（DeptUserResolveService）
 *
 * 职责：在 BPMN 流程的 SpEL 表达式中提供"按部门动态选审批人"能力，例如：
 *
 *   <userTask flowable:assignee="${deptUserResolveService.getDeptLeader(initiator)}"/>
 *   <userTask flowable:assignee="${deptUserResolveService.getDirectLeader(initiator)}"/>
 *
 * 当前实现说明（v1）：
 * - 依据峻松约定：SysDept.leader 字段存的是【负责人 username】（在系统内是 SysUser.userName）
 * - 直属上级 = 用户当前部门的 leader
 * - 部门负责人 = 用户当前部门的 leader（早期与直属上级等价；待 SysDept 拆分后再分化）
 *
 * 后续扩展（v2 路线）：
 * - 在 sys_user 增加 leader_user_id 字段或在 sys_dept 增加 leader_user_id 字段（替代字符串 leader）
 * - 在 system 模块新增 RemoteDeptService.getDeptById / getDeptUsers
 * - 此处实现按 deptId 真正反查
 *
 * @author junsong
 */
@Service("deptUserResolveService")
public class DeptUserResolveService {

    @Autowired
    private PeerUserService peerUserService;

    /**
     * 获取某用户的【直属上级】username
     * 当前规则：直属上级 = 用户所在部门的 leader 字段（peerUserService 假定 leader 存的是 username）
     */
    public String getDirectLeader(String username) {
        return getDeptLeader(username);
    }

    /**
     * 获取某用户的【部门负责人】username（与 getDirectLeader 当前一致，留出未来分化空间）
     */
    public String getDeptLeader(String username) {
        SysUser user = peerUserService.requireActiveUser(username);
        SysDept dept = user.getDept();
        if (dept == null) {
            // 退回到授权部门列表的第一个（多店授权场景）
            List<SysDept> depts = peerUserService.requireAuthorizedDeptsForValidatedUser(username);
            if (depts.isEmpty()) {
                throw new ServiceException("用户没有归属或授权部门: " + username);
            }
            dept = depts.get(0);
        }
        String leader = dept.getLeader();
        if (leader == null || leader.isBlank()) {
            throw new ServiceException("部门未配置负责人: " + dept.getDeptId());
        }
        return peerUserService.requireUsername(leader.trim());
    }

    /**
     * 给当前用户解析"上级链"——逐级向上找直至找到非空且与自己不同的 leader
     * 用于"两级审批"场景。
     */
    public String getSecondLevelLeader(String username) {
        String first = getDirectLeader(username);
        if (first == null || first.equals(username)) {
            return null;
        }
        String second = getDirectLeader(first);
        if (second == null || second.equals(first)) {
            return null;
        }
        return second;
    }

    /**
     * 按角色 key 查询该角色下的所有用户 username 列表
     */
    public List<String> getUsersByRoleCode(String roleKey) {
        return peerUserService.listUsernamesByRoleKey(roleKey);
    }

    /**
     * 按部门ID查询该部门下的所有用户 username 列表
     * 当前系统未提供按部门批量查用户的远程接口，返回空列表
     */
    public List<String> getUsersByDeptId(String deptId) {
        return List.of();
    }

    /**
     * 用于 BPMN 网关条件 / 候选人计算等场景：判断用户是否属于某个 roleKey
     * 当前因 RemoteUserService 仅暴露 username 查询入口，已直接通过 LoginUser.permissions 解析
     */
    public boolean hasRole(String username, String roleKey) {
        if (username == null || roleKey == null) {
            return false;
        }
        return peerUserService.getSysUserByUsername(username)
                .map(u -> u.getRoles() != null && u.getRoles().stream()
                        .anyMatch(r -> roleKey.equalsIgnoreCase(r.getRoleKey())))
                .orElse(false);
    }
}
