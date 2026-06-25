/**
 * JunSong-Cloud 全链路性能压测主入口
 *
 * 使用方式:
 *   冒烟测试:  k6 run -e SCENARIO=smoke main.js
 *   负载测试:  k6 run -e SCENARIO=load main.js
 *   压力测试:  k6 run -e SCENARIO=stress main.js
 *   峰值测试:  k6 run -e SCENARIO=spike main.js
 *
 * 自定义环境:
 *   k6 run -e BASE_URL=http://192.168.1.100:8080 -e TEST_USERNAME=admin -e TEST_PASSWORD=xxx main.js
 */
import { login } from './login.js'
import {
  STAGES_SMOKE,
  STAGES_LOAD,
  STAGES_STRESS,
  STAGES_SPIKE,
  THRESHOLDS,
} from './config.js'
import {
  lowcodeList,
  lowcodeDetail,
  lowcodeMetadata,
  lowcodeSubmit,
} from './scenarios/lowcode.js'
import {
  financeSaleList,
  financePurchaseList,
  financeExpenseList,
  financeInvestorList,
} from './scenarios/finance.js'
import {
  memberList,
  memberDashboard,
  seckillList,
  seckillGrab,
} from './scenarios/member.js'

const scenario = __ENV.SCENARIO || 'smoke'

function getStages() {
  switch (scenario) {
    case 'load': return STAGES_LOAD
    case 'stress': return STAGES_STRESS
    case 'spike': return STAGES_SPIKE
    default: return STAGES_SMOKE
  }
}

export const options = {
  stages: getStages(),
  thresholds: THRESHOLDS,
}

export function setup() {
  console.log(`[setup] 场景: ${scenario}, 目标URL: ${__ENV.BASE_URL || 'http://localhost:8080'}`)
  const token = login()
  console.log('[setup] 登录成功，获取到 Token')
  return { token }
}

export default function (data) {
  const token = data.token

  const rand = Math.random()

  if (rand < 0.30) {
    lowcodeList(token)
  } else if (rand < 0.40) {
    lowcodeDetail(token)
  } else if (rand < 0.50) {
    lowcodeMetadata(token)
  } else if (rand < 0.55) {
    lowcodeSubmit(token)
  } else if (rand < 0.65) {
    financeSaleList(token)
  } else if (rand < 0.70) {
    financePurchaseList(token)
  } else if (rand < 0.75) {
    financeExpenseList(token)
  } else if (rand < 0.80) {
    financeInvestorList(token)
  } else if (rand < 0.90) {
    memberList(token)
  } else if (rand < 0.95) {
    memberDashboard(token)
  } else if (rand < 0.98) {
    seckillList(token)
  } else {
    seckillGrab(token)
  }
}
