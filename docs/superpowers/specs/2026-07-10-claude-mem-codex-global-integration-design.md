# CLAUDE-MEM 与 Codex 全局集成设计

## 目标

让 Claude Code 与所有 Codex 项目共享同一个 CLAUDE-MEM 记忆库，同时满足以下要求：

- Claude Code 能正常加载 `claude-mem@thedotmack` 插件并通过 hooks 写入记忆。
- Codex 的所有会话自动写入同一个 CLAUDE-MEM 数据库。
- Codex 可通过 MCP 工具按需执行 `search`、`timeline` 和 `get_observations`。
- 历史决策、故障和项目约定可被召回，但普通简单任务不强制查询，避免不必要的延迟和上下文消耗。
- 个人数据库、令牌和本机路径配置不进入业务仓库。

## 已确认现状

- CLAUDE-MEM 版本为 `12.4.7`，用户级安装缓存存在。
- Claude Code 已启用 `claude-mem@thedotmack`，但插件列表报告 marketplace 无法解析，插件加载失败。
- CLAUDE-MEM worker 使用 `127.0.0.1:37701`。
- `CLAUDE_MEM_TRANSCRIPTS_ENABLED` 已启用。
- transcript watcher 已监听 `~/.codex/sessions/**/*.jsonl`，且状态文件已经记录当前 Codex 会话，说明 Codex 自动采集链路已开始工作。
- CLAUDE-MEM 安装包包含 stdio MCP server：`scripts/mcp-server.cjs`。
- Codex 全局配置 `~/.codex/config.toml` 已使用 `[mcp_servers.*]` 注册其他 MCP server，可沿用同一配置模式。

## 方案选择

采用“共享 worker + Codex MCP + transcript 自动采集”方案。

不单独复制或同步数据库。Claude Code hooks、Codex transcript watcher 和 Codex MCP 均连接同一个 `~/.claude-mem` 数据目录，从源头避免双库漂移。

暂不创建自定义 Codex 插件。CLAUDE-MEM 已具备 Codex transcript watcher 和 MCP server，自定义插件会重复现有能力，并增加版本升级时的维护成本。

## 架构

```text
Claude Code hooks ───────┐
                        ├── CLAUDE-MEM worker :37701 ── ~/.claude-mem/claude-mem.db
Codex transcript watcher ┘                         └── vector-db

Codex MCP client ── stdio mcp-server.cjs ── CLAUDE-MEM worker
```

写入链路：

1. Claude Code 通过 CLAUDE-MEM 插件 hooks 提交会话观察和摘要。
2. CLAUDE-MEM transcript watcher 增量读取全部 Codex JSONL 会话。
3. 两端数据写入相同数据库，并以项目路径/项目名称保留检索边界。

读取链路：

1. Codex 通过全局 MCP 配置启动 CLAUDE-MEM stdio server。
2. MCP server 连接现有 worker。
3. Codex 先使用 `search` 获取精简索引，必要时使用 `timeline`，最后批量调用 `get_observations` 获取完整内容。

## 配置变更

### Claude Code

修复 `~/.claude/settings.json` 中 marketplace 与插件缓存的注册一致性。优先使用现有 12.4.7 本地缓存，只有本地缓存不可恢复时才重新运行官方安装流程。

修复后必须确认：

- `claude plugin list` 显示插件成功加载。
- hooks 指向有效的 CLAUDE-MEM 插件根目录。
- worker 使用设置文件中的 37701 端口。

### Codex

在 `~/.codex/config.toml` 新增全局 MCP server，使用绝对路径调用当前 CLAUDE-MEM 缓存中的 `scripts/mcp-server.cjs`。环境变量显式提供数据目录和 worker 端口，避免 MCP 子进程因 shell 环境差异连接到默认 37777。

配置作用于所有 Codex 项目，不在 JunSong-Cloud 的项目级 `.codex/config.toml` 重复注册。

### 全局召回约定

在 `~/.codex/AGENTS.md` 添加 CLAUDE-MEM 使用约定：

- 当任务涉及历史实现、旧故障、先前决策、跨会话约定或用户明确要求回忆时，先查询 CLAUDE-MEM。
- 采用 `search → timeline → get_observations` 的渐进式读取方式。
- 搜索时优先限定当前项目，避免不同项目的相似术语互相污染。
- 简单、无历史依赖的任务不强制搜索。
- 记忆只能作为历史上下文；当前代码、测试结果和用户本轮指令优先。

## 安全与隐私

- `~/.claude-mem`、`~/.claude` 和 `~/.codex` 均为个人配置，不提交到业务仓库。
- 不在设计或日志中输出 API key、认证令牌或数据库内容。
- MCP 仅连接 `127.0.0.1` worker，不暴露公网监听。
- 自动采集沿用 CLAUDE-MEM 的排除项目配置；需要排除敏感仓库时通过 `CLAUDE_MEM_EXCLUDED_PROJECTS` 管理。
- 用户本轮明确指令始终覆盖历史记忆。

## 故障处理

- worker 未运行：通过已安装的 CLAUDE-MEM worker CLI 启动，再检查 `/api/health`。
- 端口不一致：以 `~/.claude-mem/settings.json` 中的 37701 为唯一来源，MCP 显式传递同一端口。
- MCP server 启动失败：检查缓存脚本和 Node/Bun 运行时，不修改业务仓库。
- Claude 插件仍无法解析：重新注册 marketplace；必要时重新执行 CLAUDE-MEM 官方用户级安装，但保留现有数据库。
- Codex 新工具未出现：重启 Codex 或新建任务，使全局 MCP 配置重新加载。

## 验证标准

集成完成必须满足：

1. CLAUDE-MEM worker health 检查成功，端口为 37701。
2. `claude plugin list` 不再报告 `claude-mem@thedotmack` 加载失败。
3. Codex 全局配置包含 CLAUDE-MEM MCP server，且不破坏现有 MCP 配置。
4. 新启动的 Codex 任务能发现 CLAUDE-MEM 的搜索工具。
5. `search` 能检索到现有记忆索引。
6. Codex 新会话内容被 transcript watcher 增量处理，状态文件偏移量更新。
7. JunSong-Cloud 工作区除本设计文档外，不产生 CLAUDE-MEM 数据或个人配置文件。

## 非目标

- 不修改 CLAUDE-MEM 源码。
- 不建设新的远程记忆服务。
- 不在项目仓库中保存个人记忆快照。
- 不对每一次 Codex 请求强制执行记忆搜索。
