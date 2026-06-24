import request from '../request'

// 动作定义
export interface LcBizAction {
  id?: number
  bizCode?: string
  actionCode: string
  actionName: string
  actionType: string
  triggerStatus: string
  apiEndpoint?: string
  buttonStyle: string
  buttonIcon?: string
  sortOrder: number
  status?: string
}

// 后置动作
export interface LcBizPostAction {
  id?: number
  bizCode?: string
  triggerEvent: string
  actionType: string
  targetField?: string
  targetValue?: string
  conditionExpr?: string
  callbackUrl?: string
  sortOrder: number
  status?: string
}

// 业务对象列表
export function listBizObject(query: any) {
  return request({ url: '/workflow/lowcode/meta/object/list', method: 'get', params: query })
}

// 读取完整配置聚合体
export function getBizConfig(bizCode: string) {
  return request({ url: '/workflow/lowcode/meta/config/' + bizCode, method: 'get' })
}

// 事务性保存完整配置
export function saveBizConfig(data: any) {
  return request({ url: '/workflow/lowcode/meta/config', method: 'post', data })
}

// 删除业务对象
export function delBizObject(ids: number | number[]) {
  return request({ url: '/workflow/lowcode/meta/object/' + ids, method: 'delete' })
}

// 一键生成菜单/权限
export function generateMenu(data: { bizCode: string; bizName: string; icon?: string }) {
  return request({ url: '/workflow/lowcode/meta/menu/generate', method: 'post', data })
}

// 配置发布快照
export interface LcBizConfigSnapshot {
  id?: number
  bizCode?: string
  versionNo: number
  configJson?: string
  status?: string
  publishRemark?: string
  createBy?: string
  createTime?: string
  remark?: string
}

// 发布当前草稿配置为新版本
export function publishConfig(bizCode: string, remark?: string) {
  return request({ url: '/workflow/lowcode/meta/config/publish/' + bizCode, method: 'post', data: { remark } })
}

// 回滚到指定版本
export function rollbackConfig(bizCode: string, versionNo: number) {
  return request({ url: '/workflow/lowcode/meta/config/rollback/' + bizCode + '?versionNo=' + versionNo, method: 'post' })
}

// 查询历史发布版本
export function listConfigHistory(bizCode: string) {
  return request({ url: '/workflow/lowcode/meta/config/history/' + bizCode, method: 'get' })
}

// 读取指定版本快照
export function getConfigSnapshot(bizCode: string, versionNo: number) {
  return request({ url: '/workflow/lowcode/meta/config/snapshot/' + bizCode + '/' + versionNo, method: 'get' })
}

// ===== 模板管理 =====
export interface LcBizTemplate {
  id?: number
  templateCode?: string
  templateName: string
  category?: string
  description?: string
  thumbnail?: string
  processKey?: string
  configJson?: string
  usageCount?: number
  isStarter?: string
  delFlag?: string
  createBy?: string
  createTime?: string
  remark?: string
}

// 模板列表
export function listTemplate(category?: string) {
  return request({ url: '/workflow/lowcode/meta/template/list', method: 'get', params: { category } })
}

// 模板详情（含 configJson）
export function getTemplate(id: number) {
  return request({ url: '/workflow/lowcode/meta/template/' + id, method: 'get' })
}

// 从模板克隆业务
export function cloneTemplate(data: { templateId: number; newBizCode: string; newBizName: string }) {
  return request({ url: '/workflow/lowcode/meta/template/clone', method: 'post', data })
}

// 保存当前业务为模板
export function saveAsTemplate(data: { bizCode: string; templateName: string; category: string; description?: string }) {
  return request({ url: '/workflow/lowcode/meta/template/save', method: 'post', data })
}
