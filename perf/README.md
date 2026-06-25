# JunSong-Cloud 全链路性能压测

基于 [k6](https://k6.io/) 的全链路 API 性能压测套件。

## 前置要求

- 安装 k6: `brew install k6` (macOS) 或参考[官方文档](https://k6.io/docs/get-started/installation/)
- 后端服务已启动（Gateway + 各模块）
- 默认使用 `admin/admin123` 账号登录获取 Token

## 目录结构

```
perf/
  config.js           # 全局配置（URL、阈值、阶段定义）
  login.js            # 登录辅助模块
  main.js             # 压测主入口
  scenarios/
    lowcode.js        # 低代码模块场景
    finance.js        # 财务模块场景
    member.js         # 会员模块场景
```

## 快速开始

### 1. 冒烟测试（1 并发，验证脚本正确性）

```bash
k6 run main.js
```

### 2. 负载测试（50→100 并发，阶梯加压）

```bash
k6 run -e SCENARIO=load main.js
```

### 3. 压力测试（100→500 并发，寻找瓶颈）

```bash
k6 run -e SCENARIO=stress main.js
```

### 4. 峰值测试（瞬间 1000 并发，验证熔断/限流）

```bash
k6 run -e SCENARIO=spike main.js
```

## 自定义参数

```bash
k6 run \
  -e BASE_URL=http://192.168.1.100:8080 \
  -e TEST_USERNAME=admin \
  -e TEST_PASSWORD=yourpassword \
  -e SCENARIO=stress \
  main.js
```

## 压测指标说明

| 指标 | 阈值 | 说明 |
|------|------|------|
| http_req_duration | p(95)<500ms | 95% 请求响应时间 < 500ms |
| http_req_duration | p(99)<1000ms | 99% 请求响应时间 < 1s |
| http_req_failed | rate<5% | 错误率 < 5% |

## 流量分布

| 接口类型 | 占比 | 说明 |
|----------|------|------|
| 低代码列表查询 | 30% | 最频繁的读操作 |
| 低代码详情 | 10% | 单条记录查询 |
| 低代码元数据 | 10% | 配置读取（可缓存） |
| 低代码提交 | 5% | 写操作 |
| 财务各模块查询 | 20% | 销售/采购/费用/投资人 |
| 会员列表/看板 | 15% | 会员相关读操作 |
| 秒杀查询/抢购 | 10% | 高并发敏感场景 |

## 输出报告

k6 内置多种输出格式：

```bash
# JSON 输出（用于后续分析）
k6 run --out json=perf-results.json main.js

# InfluxDB + Grafana 实时监控
k6 run --out influxdb=http://localhost:8086/k6 main.js

# 云端查看（需注册 k6 Cloud）
k6 cloud run main.js
```
