package com.junsong.common.core.constant;

/**
 * 缓存常量信息
 * 
 * @author junsong
 */
public class CacheConstants
{
    /**
     * 缓存有效期，默认720（分钟）
     */
    public final static long EXPIRATION = 720;

    /**
     * 缓存刷新时间，默认120（分钟）
     */
    public final static long REFRESH_TIME = 120;

    /**
     * 密码最大错误次数
     */
    public final static int PASSWORD_MAX_RETRY_COUNT = 5;

    /**
     * 密码锁定时间，默认10（分钟）
     */
    public final static long PASSWORD_LOCK_TIME = 10;

    /**
     * 权限缓存前缀
     */
    public static final String LOGIN_TOKEN_KEY = "login_tokens:";

    /**
     * 单点登录 key：userId -> token uuid
     */
    public static final String USER_TOKEN_KEY = "user_tokens:";

    /**
     * 单点登录 key：小程序端 userId -> token uuid
     */
    public static final String USER_TOKEN_MP_KEY = "user_tokens:mp:";

    /**
     * 验证码 redis key
     */
    public static final String CAPTCHA_CODE_KEY = "captcha_codes:";

    /**
     * 参数管理 cache key
     */
    public static final String SYS_CONFIG_KEY = "sys_config:";

    /**
     * 字典管理 cache key
     */
    public static final String SYS_DICT_KEY = "sys_dict:";

    /**
     * 登录账户密码错误次数 redis key
     */
    public static final String PWD_ERR_CNT_KEY = "pwd_err_cnt:";

    /**
     * 登录IP黑名单 cache key
     */
    public static final String SYS_LOGIN_BLACKIPLIST = SYS_CONFIG_KEY + "sys.login.blackIPList";

    /**
     * 行政区域 cache key
     */
    public static final String SYS_REGION_KEY = "sys_region:";

    /**
     * 岗位列表 cache key
     */
    public static final String SYS_POST_KEY = "sys_post:";

    /**
     * 菜单 cache key
     */
    public static final String SYS_MENU_KEY = "sys_menu:";

    /**
     * 微信会话版本号 key：每个租户一个原子递增的 epoch
     * 用于一键使指定租户的所有微信登录会话失效
     */
    public static final String WECHAT_SESSION_EPOCH_KEY = "wechat:session:epoch:";
}
