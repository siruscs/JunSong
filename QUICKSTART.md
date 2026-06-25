# 快速开始

> **作者：** Genesis·峻松
> **更新日期：** 2026-06-24

**目标**：5 分钟让 JunSong-Cloud 在本机跑起来，看到登录页。

---

## 前置条件

| 工具 | 版本 | 验证命令 |
|------|------|---------|
| Docker & Docker Compose | 20.10+ | `docker --version` |
| Git | 任意 | `git --version` |
| 浏览器 | 任意 | - |

> 不需要安装 JDK、Maven、Node.js——全部在 Docker 容器中完成。

---

## 一步启动（复制粘贴即可）

```bash
# 1. 克隆仓库
git clone https://github.com/siruscs/JunSong.git
cd JunSong

# 2. 一键启动全部服务（约需 3-5 分钟）
cd docker
sh deploy.sh base   # 启动 MySQL + Redis + Nacos
sleep 60            # 等 Nacos 就绪
sh deploy.sh all    # 启动所有业务服务 + 前端

# 3. 验证
open http://localhost       # 或浏览器手动打开
# 账号: admin  密码: admin123
```

---

## 看到什么算成功

| 验证项 | 预期结果 |
|--------|---------|
| 浏览器打开 `http://localhost` | 看到登录页，背景为 JunSong-Cloud |
| 登录 `admin` / `admin123` | 进入管理后台首页，左侧有菜单栏 |
| Nacos 控制台 `http://localhost:7080/next` | 登录后看到服务列表里有 10+ 个注册服务 |

---

## 常见问题（30 秒解决）

| 现象 | 解决 |
|------|------|
| `deploy.sh` 命令找不到 | `chmod +x docker/deploy.sh` 后再执行 |
| Nacos 启动慢 | 首次启动需 45-60 秒初始化数据库，耐心等待 |
| 端口被占用 | 检查本地是否有 MySQL(3306)/Redis(6379)/Nginx(80) 在运行 |
| 页面白屏 | 按 `F12` 看控制台，确认 `http://localhost` 能访问而非 `https` |

---

## 下一步

- 📖 [部署运维手册](./部署运维手册.md) — 深入了解各服务、按需启动、更新流程
- 📖 [二次开发指南](./二次开发指南.md) — 学习如何新增一个业务服务
- 📖 [低代码配置手册](./docs/lowcode-delivery/operation/config-admin-manual.md) — 配置审批流程

---

*本快速开始由 Genesis·峻松 维护*
