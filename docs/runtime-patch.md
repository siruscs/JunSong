# 微信小程序运行时 Patch 说明

当前构建固定使用 `@dcloudio/uni-mp-weixin` `3.0.0-5000720260410001`。

## Patch 内容

`vite.config.js` 中的 `patch-uni-mp-weixin-runtime` 用于：

- 替换构建运行时对 `wx.getSystemInfoSync()`、`getAppBaseInfo`、`getWindowInfo`、`getDeviceInfo` 的启动期调用，避免微信开发者工具启动阶段触发系统信息权限提示或异常。
- 禁用 `preloadAsset()`，避免空闲阶段 `wx.preloadAssets` 超时影响首屏。
- 复制 tab 图标和项目配置，保证构建产物与项目配置一致。

## 验证方式

升级 uni-app 相关依赖前，必须执行：

```bash
npm run build:mp-weixin
node --test test/error-reporter-v12.test.mjs
```

并在微信开发者工具验证冷启动、后台回前台、登录页和首页。若新版运行时不再包含上述调用，才可以移除对应正则 Patch；否则保留并更新本说明。
