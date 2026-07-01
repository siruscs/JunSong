import fs from 'node:fs'
import path from 'node:path'

const root = process.cwd()

const checks = [
  {
    file: 'junsong-ui-v3/src/views/module-overview/ModuleOverview.vue',
    includes: [
      'module-overview',
      'quickLinks',
      'governance',
      'nextSteps',
      'signals',
      'watchlist',
      'cadence',
      '状态信号',
      '巡检清单',
      '运营节奏',
    ],
  },
  {
    file: 'junsong-ui-v3/src/views/system/overview/index.vue',
    includes: ['系统管理概览', '租户初始化', '/system/tenant', '/system/user', '/system/menu', '菜单路由健康', '平台权限边界'],
  },
  {
    file: 'junsong-ui-v3/src/views/finance/overview/index.vue',
    includes: [
      '财务管理概览',
      '日结闭环',
      '/finance/expense',
      '/finance/costAccounting',
      '/finance/profitShare',
      '未核销费用',
      '期间锁账',
    ],
  },
  {
    file: 'junsong-ui-v3/src/views/member/overview/index.vue',
    includes: ['会员管理概览', '会员详情', '/member/member', '/member/pointsRule', '/member/mpPerm', '积分一致性', '小程序权限'],
  },
  {
    file: 'sql/admin_module_overview_menus.sql',
    includes: [
      '系统概览',
      '财务概览',
      '会员概览',
      'system/overview/index',
      'finance/overview/index',
      'member/overview/index',
      '会员管理',
    ],
  },
]

const failures = []

for (const check of checks) {
  const fullPath = path.join(root, check.file)
  if (!fs.existsSync(fullPath)) {
    failures.push(`Missing file: ${check.file}`)
    continue
  }

  const content = fs.readFileSync(fullPath, 'utf8')
  for (const expected of check.includes) {
    if (!content.includes(expected)) {
      failures.push(`Missing "${expected}" in ${check.file}`)
    }
  }
}

if (failures.length) {
  console.error('Module overview verification failed:')
  failures.forEach((failure) => console.error(`- ${failure}`))
  process.exit(1)
}

console.log('Module overview verification passed.')
