package com.junsong.system.mapper;

import java.util.List;
import com.junsong.system.api.domain.SysUserMpBinding;

/**
 * 小程序微信账号绑定关系表 数据层
 *
 * <p>所有查询方法必须显式带 tenantId 参数，禁止仅按 openid 查询。</p>
 * <p>解绑使用 {@link #revoke} 更新 status 为 REVOKED，不提供物理 delete 方法。</p>
 */
public interface SysUserMpBindingMapper
{
    /**
     * 按 (tenantId, appId, openid) 查询 ACTIVE 绑定关系
     *
     * @param tenantId 租户ID（必填）
     * @param appId    微信 AppID（必填）
     * @param openid   微信 openid（必填）
     * @return ACTIVE 绑定关系，不存在返回 null
     */
    public SysUserMpBinding selectByAppOpenid(Long tenantId, String appId, String openid);

    /**
     * 仅按 (appId, openid) 全局查询 ACTIVE 绑定关系（用于微信快捷登录）。
     *
     * <p>此方法不传 tenantId，因为登录时尚不知道租户归属。
     * (app_id, openid) 全局唯一键保证最多返回一条记录，从结果中获取 tenantId。
     * 此方法仅限登录流程使用，其他场景必须使用 {@link #selectByAppOpenid}。</p>
     *
     * @param appId  微信 AppID（必填）
     * @param openid 微信 openid（必填）
     * @return ACTIVE 绑定关系，不存在返回 null
     */
    public SysUserMpBinding selectActiveByAppOpenidForLogin(String appId, String openid);

    /**
     * 按 (tenantId, userId) 查询绑定列表（管理员查看）
     *
     * @param tenantId 租户ID（必填）
     * @param userId   用户ID（必填）
     * @return 该用户在该租户下的所有绑定关系（含 ACTIVE 和 REVOKED）
     */
    public List<SysUserMpBinding> selectByUserId(Long tenantId, Long userId);

    /**
     * 按 (tenantId, bindingId) 查询绑定关系
     *
     * @param tenantId  租户ID（必填）
     * @param bindingId 绑定关系ID（必填）
     * @return 绑定关系，不存在返回 null
     */
    public SysUserMpBinding selectByBindingId(Long tenantId, Long bindingId);

    /**
     * 新增绑定关系
     *
     * <p>使用唯一键 uk_user_mp_binding_app_openid 防止并发绑定同一微信身份。
     * 重复插入会抛出 DuplicateKeyException，由上层处理。</p>
     *
     * @param binding 绑定关系
     * @return 受影响行数
     */
    public int insert(SysUserMpBinding binding);

    /**
     * 撤销绑定（将 status 更新为 REVOKED，记录撤销时间和操作人）
     *
     * <p>不物理删除，保留审计链。使用条件更新（status='ACTIVE'）防止重复撤销。</p>
     *
     * @param binding 包含 bindingId、tenantId、revokedBy、revokeReason
     * @return 受影响行数（0 表示绑定不存在或已撤销）
     */
    public int revoke(SysUserMpBinding binding);

    /**
     * 更新最近一次微信快捷登录时间
     *
     * @param tenantId  租户ID
     * @param bindingId 绑定关系ID
     * @return 受影响行数
     */
    public int updateLastLoginTime(Long tenantId, Long bindingId);

    /**
     * 重新激活已撤销的绑定关系（REVOKED → ACTIVE）
     *
     * <p>解绑后状态为 REVOKED 的记录仍占用 (app_id, openid) 唯一键，
     * 重新绑定时无法 INSERT 新记录。此方法将 REVOKED 记录更新为 ACTIVE，
     * 覆盖为新的绑定用户信息，保留 create_time/create_by 原始审计痕迹。</p>
     *
     * <p>条件更新（status='REVOKED'）防止并发重复激活。
     * 返回 0 表示记录已被其他请求激活或状态已变更。</p>
     *
     * @param binding 包含新的 tenantId、userId、appId、openid、unionid、boundBy
     * @return 受影响行数（1=成功，0=竞态失败）
     */
    public int reactivate(SysUserMpBinding binding);
}
