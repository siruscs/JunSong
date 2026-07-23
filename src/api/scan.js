/**
 * 扫码查找 API。
 *
 * 安全边界：
 * 1. 扫码只负责查找，不代表拥有读取或写入权限
 * 2. 商品、会员查询必须使用当前租户和授权部门范围
 * 3. 无权限扫码结果由后端拒绝（403）
 * 4. 部门切换后旧查询结果不污染新部门（通过 deptId 参数隔离）
 *
 * 后端接口：
 * - 商品：GET /product/list?productCode=xxx 或 GET /product/{productId}
 * - 会员：GET /member/no/{memberNo}
 *
 * @module api/scan
 */
import { request } from './index.js'

/**
 * 按商品编码扫码查找商品。
 * @param {string} code 商品编码或条码
 * @param {number} deptId 当前门店ID（隔离部门范围）
 * @returns {Promise<Object>} 商品信息
 */
export function findProductByCode(code, deptId) {
  return request({
    url: '/product/list',
    method: 'GET',
    data: { productCode: code, deptId },
    silent: true
  }).then((res) => {
    const rows = res?.rows || []
    return rows.length > 0 ? rows[0] : null
  })
}

/**
 * 按商品ID查询商品详情。
 * @param {number} productId 商品ID
 * @returns {Promise<Object>} 商品详情
 */
export function findProductById(productId) {
  return request({
    url: '/product/' + productId,
    method: 'GET',
    silent: true
  })
}

/**
 * 按会员编号扫码查找会员。
 * @param {string} memberNo 会员编号
 * @returns {Promise<Object>} 会员信息（PII 已脱敏）
 */
export function findMemberByNo(memberNo) {
  return request({
    url: '/member/no/' + encodeURIComponent(memberNo),
    method: 'GET',
    silent: true
  })
}

/**
 * 按会员ID查询会员详情。
 * @param {number} memberId 会员ID
 * @returns {Promise<Object>} 会员详情
 */
export function findMemberById(memberId) {
  return request({
    url: '/member/' + memberId,
    method: 'GET',
    silent: true
  })
}
