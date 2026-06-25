# 接口文档

 > **作者：** Genesis·峻松
 > **更新日期：** 2026-06-24

本文档说明 JunSong-Cloud 核心 REST API 的调用方式。完整接口列表可通过启动后端服务后访问 Swagger UI 查看。

 ---

 ## 一、接口基础信息

 ### 1.1 基础地址

 | 环境 | 基础地址 |
 |------|---------|
 | 本地开发 | `http://localhost:8081`（Gateway 端口） |
 | 生产环境 | `https://your-domain.com` |

 > 所有请求通过 Gateway 统一入口，由 Gateway 路由转发到各微服务。

 ### 1.2 认证方式

 **Bearer Token（JWT）**

 1. 调用登录接口获取 Token
 2. 后续请求在 Header 中携带：`Authorization: Bearer {token}`

 ```http
 GET /prod-api/system/user/list HTTP/1.1
 Host: localhost
 Authorization: Bearer eyJhbGciOiJSUzI1NiJ9...
 ```

 ### 1.3 通用响应格式

 ```json
 {
   "code": 200,
   "msg": "操作成功",
   "data": { ... }
 }
 ```

 | 字段 | 类型 | 说明 |
 |------|------|------|
 | `code` | int | 200=成功，其他=错误码 |
 | `msg` | string | 提示信息 |
 | `data` | any | 业务数据（列表/对象/空） |

 **分页列表响应格式：**

 ```json
 {
   "code": 200,
   "msg": "查询成功",
   "data": {
     "total": 100,
     "rows": [ ... ]
   }
 }
 ```

 ### 1.4 公共请求头

 | Header | 必填 | 说明 |
 |--------|------|------|
 | `Authorization` | 是 | `Bearer {token}` |
 | `Content-Type` | 是 | `application/json` |

 ### 1.5 白名单接口（无需 Token）

 | 接口 | 说明 |
 |------|------|
 | `POST /auth/login` | 用户登录 |
 | `POST /auth/logout` | 用户登出 |
 | `GET /code` | 获取验证码 |
 | `GET /auth/captcha` | 图形验证码 |

 ---

 ## 二、核心模块接口

 ### 2.1 认证模块（auth）

 | 方法 | 路径 | 说明 | 权限 |
 |------|------|------|------|
 | POST | `/auth/login` | 用户登录 | 白名单 |
 | POST | `/auth/logout` | 用户登出 | 白名单 |
 | GET | `/auth/info` | 获取当前登录用户信息 | 已登录 |
 | GET | `/auth/refresh` | 刷新 Token | 已登录 |

 **登录请求：**

 ```json
 {
   "username": "admin",
   "password": "admin123",
   "code": "a3f7",
   "uuid": "captcha-uuid"
 }
 ```

 **登录成功响应：**

 ```json
 {
   "code": 200,
   "msg": "登录成功",
   "data": {
     "token": "eyJhbGciOiJSUzI1NiJ9...",
     "expires_in": 7200
   }
 }
 ```

 ### 2.2 系统管理模块（system）

 | 方法 | 路径 | 说明 | 权限 |
 |------|------|------|------|
 | GET | `/system/user/list` | 用户列表 | system:user:list |
 | GET | `/system/user/{userId}` | 用户详情 | system:user:query |
 | POST | `/system/user` | 新增用户 | system:user:add |
 | PUT | `/system/user` | 修改用户 | system:user:edit |
 | DELETE | `/system/user/{userIds}` | 删除用户 | system:user:remove |
 | GET | `/system/role/list` | 角色列表 | system:role:list |
 | GET | `/system/menu/list` | 菜单列表 | system:menu:list |
 | GET | `/system/dept/list` | 部门（门店）列表 | system:dept:list |
 | GET | `/system/dept/tree` | 部门树 | system:dept:list |
 | GET | `/system/dict/type/list` | 字典类型列表 | system:dict:list |
 | GET | `/system/dict/data/type/{dictType}` | 字典数据 | 白名单 |

 ### 2.3 低代码审批模块（lowcode）

 | 方法 | 路径 | 说明 | 权限 |
 |------|------|------|------|
 | GET | `/lowcode/meta/object/list` | 业务对象列表 | lowcode:meta:list |
 | GET | `/lowcode/meta/object/{bizCode}` | 业务对象详情 | lowcode:meta:query |
 | POST | `/lowcode/meta/object` | 新增业务对象 | lowcode:meta:add |
 | PUT | `/lowcode/meta/object` | 修改业务对象 | lowcode:meta:edit |
 | DELETE | `/lowcode/meta/object/{bizCode}` | 删除业务对象 | lowcode:meta:remove |
 | POST | `/lowcode/meta/object/publish` | 发布配置 | lowcode:meta:publish |
 | GET | `/lowcode/instance/list` | 业务实例列表 | 按业务对象权限 |
 | GET | `/lowcode/instance/{instanceId}` | 实例详情 | 按业务对象权限 |
 | POST | `/lowcode/instance` | 保存草稿 | 按业务对象权限 |
 | POST | `/lowcode/instance/submit` | 提交审批 | 按业务对象权限 |
 | POST | `/lowcode/instance/withdraw` | 撤回 | 按业务对象权限 |
 | POST | `/lowcode/instance/approve` | 审批通过 | 按业务对象权限 |
 | POST | `/lowcode/instance/reject` | 审批驳回 | 按业务对象权限 |
 | POST | `/lowcode/instance/fulfill` | 履约补录 | 按业务对象权限 |

 ### 2.4 财务管理模块（finance）

 | 方法 | 路径 | 说明 | 权限 |
 |------|------|------|------|
 | GET | `/finance/purchase/list` | 进货单列表 | finance:purchase:list |
 | POST | `/finance/purchase` | 新增进货单 | finance:purchase:add |
 | PUT | `/finance/purchase` | 修改进货单 | finance:purchase:edit |
 | DELETE | `/finance/purchase/{purchaseIds}` | 删除进货单 | finance:purchase:remove |
 | GET | `/finance/expense/list` | 费用报销列表 | finance:expense:list |
 | GET | `/finance/sale/list` | 销售记录列表 | finance:sale:list |
 | GET | `/finance/product/list` | 商品列表 | finance:product:list |
 | GET | `/finance/cost/list` | 成本核算列表 | finance:cost:list |
 | GET | `/finance/investor/list` | 投资人列表 | finance:investor:list |
 | GET | `/finance/profit/list` | 分润记录列表 | finance:profit:list |

 ### 2.5 会员管理模块（member）

 | 方法 | 路径 | 说明 | 权限 |
 |------|------|------|------|
 | GET | `/member/list` | 会员列表 | member:member:list |
 | GET | `/member/integral/list` | 积分记录 | member:integral:list |
 | GET | `/member/seckill/list` | 秒杀活动 | member:seckill:list |
 | GET | `/member/refund/list` | 退款记录 | member:refund:list |

 ### 2.6 文件服务模块（file）

 | 方法 | 路径 | 说明 | 权限 |
 |------|------|------|------|
 | POST | `/file/upload` | 文件上传 | 已登录 |
 | GET | `/file/download/{fileId}` | 文件下载 | 已登录 |
 | GET | `/file/preview/{fileId}` | 文件预览 | 已登录 |

 **上传请求（multipart/form-data）：**

 ```http
 POST /prod-api/file/upload HTTP/1.1
 Authorization: Bearer {token}
 Content-Type: multipart/form-data; boundary=----WebKitFormBoundary

 ------WebKitFormBoundary
 Content-Disposition: form-data; name="file"; filename="doc.pdf"
 Content-Type: application/pdf

 (文件内容)
 ------WebKitFormBoundary--
 ```

 ### 2.7 工作流模块（workflow）

 | 方法 | 路径 | 说明 | 权限 |
 |------|------|------|------|
 | GET | `/workflow/definition/list` | 流程定义列表 | workflow:definition:list |
 | POST | `/workflow/definition/deploy` | 部署流程 | workflow:definition:deploy |
 | GET | `/workflow/task/list` | 待办任务 | 已登录 |
 | POST | `/workflow/task/complete` | 完成任务 | 已登录 |
 | GET | `/workflow/history/list` | 审批历史 | 已登录 |

 ---

 ## 三、接口调用示例

 ### 3.1 完整调用流程（curl）

 ```bash
 # 1. 获取验证码
 curl -s "http://localhost:8081/code"

 # 2. 登录
 curl -s -X POST "http://localhost:8081/auth/login" \
   -H "Content-Type: application/json" \
   -d '{"username":"admin","password":"admin123","code":"a3f7","uuid":"xxx"}'

 # 3. 保存 Token
 TOKEN="eyJhbGciOiJSUzI1NiJ9..."

 # 4. 调用业务接口
 curl -s "http://localhost:8081/system/user/list?pageNum=1&pageSize=10" \
   -H "Authorization: Bearer $TOKEN"

 # 5. 低代码查询
 curl -s "http://localhost:8081/lowcode/meta/object/list" \
   -H "Authorization: Bearer $TOKEN"
 ```

 ### 3.2 前端调用示例（axios）

 ```javascript
 import request from '@/utils/request'

 // 查询用户列表
 export function listUser(query) {
   return request({
     url: '/system/user/list',
     method: 'get',
     params: query
   })
 }

 // 新增用户
 export function addUser(data) {
   return request({
     url: '/system/user',
     method: 'post',
     data
   })
 }
 ```

 ---

 ## 四、Swagger / Knife4j 在线文档

 启动后端服务后，可通过以下地址查看完整接口文档：

 | 地址 | 说明 |
 |------|------|
 | `http://localhost:8081/doc.html` | Knife4j 增强版 Swagger UI（需启动 Gateway） |
 | `http://localhost:{port}/swagger-ui.html` | 各服务原生 Swagger（如 9201 system） |

 > 生产环境建议关闭 Swagger 或限制访问。

 ---

 ## 五、错误码说明

 | 错误码 | 说明 |
 |--------|------|
 | 200 | 成功 |
 | 401 | 未认证（Token 无效或过期） |
 | 403 | 无权限 |
 | 404 | 资源不存在 |
 | 500 | 服务器内部错误 |
 | 1001 | 验证码错误 |
 | 1002 | 用户不存在或密码错误 |
 | 1003 | Token 已过期 |

 ---

 *本文档由 Genesis·峻松 维护，最后更新：2026-06-24*
