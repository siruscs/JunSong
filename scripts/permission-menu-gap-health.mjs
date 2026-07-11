#!/usr/bin/env node
/**
 * 权限缺口扫描脚本
 * 对比后端 @RequiresPermissions 注解和数据库 sys_menu.perms，输出缺口清单
 * 用法: node scripts/permission-menu-gap-health.mjs [dev|prod]
 */
import { execSync, spawnSync } from 'node:child_process';
import { readFileSync, readdirSync, statSync, existsSync } from 'node:fs';
import { join, extname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { dirname } from 'node:path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const PROJECT_ROOT = join(__dirname, '..');
const ENV = process.argv[2] || 'dev';

// 部署凭据从环境变量读取，避免在治理脚本里硬编码 SSH 主机/数据库密码
const PROD_SSH_HOST = process.env.PROD_SSH_HOST || '';
const MYSQL_ROOT_PASSWORD = process.env.MYSQL_ROOT_PASSWORD || '';
const MYSQL_DATABASE = process.env.MYSQL_DATABASE || 'junsong-cloud';
const MYSQL_CONTAINER = process.env.MYSQL_CONTAINER || 'junsong-mysql';

// 全局豁免权限清单（不挂菜单，DEV/PROD 均豁免，已在 permission_menu_gap_fix.sql 中说明）
const EXEMPT_PERMS = [
  /^system:file:/,                    // 文件上传是内部能力
  /^open:foundation:/,                // 平台公共基础接口
  /^open:member:points:write$/,       // 外部开放 API 写权限
  /^lowcode:/,                        // 低代码业务权限模型待决策
  /^workflow:mobile:/,                // 移动办公权限
  /^gen:table:list$/,                 // 命名不一致，应改代码
  /^monitor:operlog:/,                // 前端 v-hasPermi 备用写法
  /^monitor:logininfor:/,             // 前端 v-hasPermi 备用写法
];

// PROD 专属豁免（仅 PROD 环境豁免；DEV 不豁免，避免掩盖 DEV 真实缺口）
const PROD_EXEMPT_PERMS = [
  /^system:tenant:/,                  // PROD 单租户环境，暂不启用租户管理
  /^system:workbench:notify$/,        // PROD 暂未启用工作台通知
];

// 根据环境组合豁免清单
const activeExemptPerms = ENV === 'prod'
  ? [...EXEMPT_PERMS, ...PROD_EXEMPT_PERMS]
  : EXEMPT_PERMS;

// 1. 扫描后端 Java 代码中的 @RequiresPermissions 注解
function scanBackendPermissions() {
  const perms = new Set();
  const javaDirs = [
    join(PROJECT_ROOT, 'junsong-modules'),
    join(PROJECT_ROOT, 'junsong-admin'),
  ];

  function scanDir(dir) {
    if (!existsSync(dir)) return;
    for (const entry of readdirSync(dir)) {
      const fullPath = join(dir, entry);
      const stat = statSync(fullPath);
      if (stat.isDirectory()) {
        scanDir(fullPath);
      } else if (extname(fullPath) === '.java') {
        const content = readFileSync(fullPath, 'utf8');
        // 匹配 @RequiresPermissions("xxx") 和 @RequiresPermissions({"xxx","yyy"})
        const regex = /@RequiresPermissions\s*\(\s*(?:\{|")([^")\]}]+)(?:[",)\]}])/g;
        let match;
        while ((match = regex.exec(content)) !== null) {
          const perm = match[1].trim();
          if (perm && !perm.includes('${')) {
            perms.add(perm);
          }
        }
      }
    }
  }

  javaDirs.forEach(scanDir);
  return [...perms].sort();
}

// 2. 查询数据库中的权限菜单
// DB 查询失败直接退出（exit 2），不返回空数组继续做差集，避免误导性的"全部缺失"结论
function queryDbPermissions() {
  const isProd = ENV === 'prod';

  if (!MYSQL_ROOT_PASSWORD) {
    console.error(`错误: 必须设置环境变量 MYSQL_ROOT_PASSWORD（脚本不再保留默认密码）`);
    console.error(`用法: MYSQL_ROOT_PASSWORD=<pwd> node scripts/permission-menu-gap-health.mjs dev`);
    console.error(`      PROD_SSH_HOST=root@<ip> MYSQL_ROOT_PASSWORD=<pwd> node scripts/permission-menu-gap-health.mjs prod`);
    process.exit(2);
  }

  const innerSql = `SELECT DISTINCT perms FROM sys_menu WHERE perms IS NOT NULL AND perms != ''`;
  const localMysql = `docker exec -i ${MYSQL_CONTAINER} mysql -uroot -p${MYSQL_ROOT_PASSWORD} --default-character-set=utf8mb4 -N -B ${MYSQL_DATABASE} -e "${innerSql}"`;

  let mysqlCmd;
  if (isProd) {
    if (!PROD_SSH_HOST) {
      console.error(`错误: PROD 环境需要设置环境变量 PROD_SSH_HOST（例如 root@1.2.3.4）`);
      console.error(`用法: PROD_SSH_HOST=root@<ip> MYSQL_ROOT_PASSWORD=<pwd> node scripts/permission-menu-gap-health.mjs prod`);
      process.exit(2);
    }
    const remoteSql = `SELECT DISTINCT perms FROM sys_menu WHERE perms IS NOT NULL AND perms != \\"\\"`;
    mysqlCmd = `ssh ${PROD_SSH_HOST} 'docker exec -i ${MYSQL_CONTAINER} mysql -uroot -p${MYSQL_ROOT_PASSWORD} --default-character-set=utf8mb4 -N -B ${MYSQL_DATABASE} -e "${remoteSql}"'`;
  } else {
    mysqlCmd = localMysql;
  }

  let result;
  try {
    result = spawnSync('bash', ['-c', mysqlCmd], { encoding: 'utf8' });
  } catch (e) {
    console.error(`数据库查询异常: ${e.message}`);
    process.exit(2);
  }

  if (result.error) {
    console.error(`数据库查询无法启动: ${result.error.message}`);
    process.exit(2);
  }
  if (result.status !== 0) {
    console.error(`数据库查询失败 (exit ${result.status}): ${result.stderr || result.stdout}`);
    process.exit(2);
  }

  const rows = result.stdout.trim().split('\n').map(p => p.trim()).filter(p => p);
  if (rows.length === 0) {
    console.error(`数据库查询结果为空，可能连接异常或数据库无权限数据，终止以避免误导`);
    process.exit(2);
  }
  return rows.sort();
}

// 3. 主流程
console.log(`==========================================`);
console.log(`  权限缺口扫描 - ${ENV.toUpperCase()} 环境`);
console.log(`==========================================\n`);

const backendPerms = scanBackendPermissions();
const dbPerms = queryDbPermissions();

console.log(`后端代码权限数: ${backendPerms.length}`);
console.log(`数据库菜单权限数: ${dbPerms.length}\n`);

// 找出后端有但数据库没有的权限
const dbPermSet = new Set(dbPerms);
const missing = backendPerms.filter(p => !dbPermSet.has(p));

// 分离豁免权限和真正缺失的权限
const exempt = [];
const realMissing = [];
for (const p of missing) {
  if (activeExemptPerms.some(re => re.test(p))) {
    exempt.push(p);
  } else {
    realMissing.push(p);
  }
}

console.log(`=== 缺失权限分析 ===`);
console.log(`总缺失: ${missing.length}`);
console.log(`豁免数: ${exempt.length}`);
console.log(`真实缺失: ${realMissing.length}\n`);

if (exempt.length > 0) {
  console.log(`=== 豁免权限清单 ===`);
  exempt.forEach(p => console.log(`  [豁免] ${p}`));
  console.log('');
}

if (realMissing.length > 0) {
  console.log(`=== 真实缺失权限（需补菜单或加入豁免）===`);
  realMissing.forEach(p => console.log(`  [缺失] ${p}`));
  console.log('');
  console.log(`结论: 存在 ${realMissing.length} 个真实缺失权限，需处理`);
  process.exit(1);
} else {
  console.log(`结论: 权限缺口已清零（缺失数=0 或全部进入豁免清单）`);
  process.exit(0);
}
