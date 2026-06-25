# 性能基准与监控

> **作者：** Genesis·峻松
> **更新日期：** 2026-06-24

本文档说明 JunSong-Cloud 各服务的性能参考指标、监控方式与调优建议。

---

## 一、环境基准

以下数据基于**最小启动集**（7 服务：MySQL + Redis + Nacos + Gateway + Auth + System + Nginx）在本地 Docker Desktop 环境测得，仅供参考。实际生产环境性能取决于硬件配置、网络条件与并发量。

| 指标 | 参考值 | 说明 |
|------|--------|------|
| 系统启动时间 | 45–60 秒 | 含 Nacos 初始化 |
| 单次服务重启 | 8–15 秒 | docker restart |
| 登录接口（/auth/login） | < 200ms | P95 |
| 用户列表查询（/system/user/list） | < 300ms | 100 条/页，P95 |
| 低代码实例列表（/lowcode/instance/list） | < 500ms | 含 JSON 解析，P95 |
| 文件上传（/file/upload） | < 2s | 10MB 文件 |
| 前端首屏加载 | < 3s | 首次访问，无缓存 |

---

## 二、各服务资源占用参考

| 服务 | 内存（容器） | CPU | 说明 |
|------|-------------|-----|------|
| junsong-mysql | 512MB–1GB | 0.5–1 核 | 随数据量增长 |
| junsong-redis | 128MB–256MB | 0.1–0.3 核 | |
| junsong-nacos | 512MB–1GB | 0.3–0.5 核 | 首次启动占用较高 |
| junsong-gateway | 256MB–512MB | 0.2–0.5 核 | 流量入口，建议预留余量 |
| junsong-auth | 256MB–512MB | 0.2–0.3 核 | |
| junsong-system | 256MB–512MB | 0.2–0.5 核 | 含地图查询时略高 |
| junsong-finance | 256MB–512MB | 0.2–0.5 核 | 报表查询时略高 |
| junsong-workflow | 256MB–512MB | 0.2–0.5 核 | Flowable 运行时 |
| junsong-member | 256MB–512MB | 0.2–0.5 核 | |
| junsong-file | 128MB–256MB | 0.1–0.3 核 | |
| junsong-nginx | 64MB–128MB | 0.1 核 | |

> **总内存估算**：最小启动集约 **2GB**，全量 13 服务约 **4–5GB**。

---

## 三、监控方式

### 3.1 Spring Boot Actuator 健康检查

各服务均暴露 Actuator 端点：

```bash
# 健康状态
curl http://localhost:{port}/actuator/health

# 示例
curl http://localhost:9201/actuator/health   # system
curl http://localhost:9205/actuator/health   # finance
curl http://localhost:8081/actuator/health   # gateway
```

### 3.2 Prometheus 指标（可选）

在 `application.yml` 中开启 Prometheus 端点：

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    tags:
      application: ${spring.application.name}
```

访问：`http://localhost:{port}/actuator/prometheus`

### 3.3 Nacos 服务健康

Nacos 控制台 → 服务管理 → 服务列表，查看各服务实例的**健康状态**（绿色 = 健康，红色 = 异常）。

### 3.4 日志监控

```bash
# 实时查看某服务日志
docker compose logs -f junsong-gateway

# 查看最近 100 行
docker compose logs --tail 100 junsong-auth

# 搜索错误
docker compose logs junsong-system | grep ERROR
```

---

## 四、常见性能问题与调优

### 4.1 Nacos 启动慢

**现象**：Nacos 首次启动需 45–60 秒。
**原因**：Nacos 初始化数据库、加载配置。
**优化**：
- 确保 MySQL 已就绪后再启动 Nacos（`depends_on`）
- 给 Nacos 分配更多内存（`environment: JVM_XMS=512m JVM_XMX=512m`）

### 4.2 Gateway 响应慢

**现象**：接口响应时间 > 1s。
**排查**：
1. 检查 Gateway 日志是否有路由转发超时
2. 检查下游服务是否健康（Nacos 服务列表）
3. 检查 Redis 连接是否正常（验证码、Token 缓存）

**优化**：
- 增加 Gateway 连接池大小
- 开启响应缓存（静态资源）
- 检查 Sentinel 限流规则是否过严

### 4.3 数据库查询慢

**现象**：列表查询 > 1s。
**排查**：
1. 检查是否缺少索引（`EXPLAIN` 分析）
2. 检查分页是否过大（PageHelper 深分页问题）
3. 检查是否命中数据权限过滤（大量 dept 递归）

**优化**：
- 为高频查询字段添加索引
- 限制单页最大条数（如 500）
- 数据权限查询优化（ dept 树缓存）

### 4.4 前端首屏加载慢

**现象**：首次访问 > 5s。
**优化**：
- 开启 Nginx gzip 压缩
- 配置浏览器缓存（静态资源）
- 按需加载 Element Plus 组件
- 使用 CDN 加速（生产环境）

---

## 五、负载测试建议

使用 **JMeter** 或 **k6** 进行压测：

```bash
# k6 示例：登录接口压测
k6 run --vus 100 --duration 30s - <<EOF
import http from 'k6/http';
export default function () {
  http.post('http://localhost:8081/auth/login', JSON.stringify({
    username: 'admin',
    password: 'admin123',
    code: '1234',
    uuid: 'test'
  }), { headers: { 'Content-Type': 'application/json' } });
}
EOF
```

**建议压测场景**：
1. 登录接口：100 并发，持续 60 秒
2. 用户列表查询：50 并发，持续 60 秒
3. 文件上传：20 并发，10MB 文件

---

## 六、扩展建议

| 场景 | 建议 |
|------|------|
| 用户量 < 1000 | 单机 Docker Compose 足够 |
| 用户量 1000–10000 | 多实例 Gateway + System，Nacos 集群 |
| 用户量 > 10000 | Kubernetes 编排，数据库主从分离，Redis 集群 |
| 高并发读 | 增加 Redis 缓存命中率，数据库读库分离 |
| 高并发写 | 异步处理（MQ），数据库分库分表 |

---

*本文档由 Genesis·峻松 维护，最后更新：2026-06-24*
