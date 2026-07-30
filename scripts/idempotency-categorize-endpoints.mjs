#!/usr/bin/env node
/**
 * 幂等治理端点分类脚本。
 *
 * 将所有 Controller 写端点（POST/PUT/DELETE/PATCH）分类为：
 * - MUTATION：需要 @Idempotent（保存/提交/审批/驳回/过账/冲销/导入/批量操作等）
 * - QUERY：跳过（/export /list /search /dashboard /board /snapshot /page /query 等查询）
 * - INTERNAL：跳过（@InnerAuth 内部端点，走 X-Inner-Token 鉴权）
 *
 * 输出：每个模块需要添加 @Idempotent 的端点清单
 */
import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join } from 'node:path';

const repoRoot = process.cwd();
const modulesDir = join(repoRoot, 'junsong-modules');

const QUERY_PATTERNS = [
  /\/export$/i,
  /\/export\//i,
  /\/list$/i,
  /\/list\//i,
  /\/search/i,
  /\/dashboard/i,
  /\/board$/i,
  /\/board\//i,
  /\/snapshot/i,
  /\/query/i,
  /\/page$/i,
  /\/page\//i,
  /\/summary$/i,
  /\/summary\//i,
  /\/reconciliation/i,
  /\/portfolio/i,
  /\/health-trend/i,
  /\/health-tasks/i,
  /\/drilldown/i,
  /\/operatingMetrics/i,
  /\/metrics/i,
  /\/report\//i,
  /\/report$/i,
  /\/forecast/i,
  /\/memo$/i,
  /\/memo\//i,
  /\/weekly-memo/i,
  /\/review-board/i,
  /\/review-quality/i,
  /\/predictive/i,
  /\/what-if/i,
  /\/alerts/i,
  /\/review-tasks$/i, // dashboard endpoint
  /\/receivable-collection\/dashboard/i,
  /\/receivable-collection\/list/i,
  /\/store\/summary/i,
  /\/store\/authorized/i,
];

function isQueryEndpoint(url) {
  return QUERY_PATTERNS.some((p) => p.test(url));
}

function findJavaFiles(dir, acc = []) {
  const entries = readdirSync(dir);
  for (const entry of entries) {
    const full = join(dir, entry);
    const st = statSync(full);
    if (st.isDirectory()) {
      findJavaFiles(full, acc);
    } else if (entry.endsWith('.java')) {
      acc.push(full);
    }
  }
  return acc;
}

function parseController(file) {
  const content = readFileSync(file, 'utf8');
  if (!/@RestController|@Controller/.test(content)) return null;

  const hasInnerAuth = /@InnerAuth/.test(content);
  const packageMatch = content.match(/package\s+([\w.]+);/);
  const pkg = packageMatch ? packageMatch[1] : '';

  // class-level @RequestMapping
  const classMapping = content.match(/@RequestMapping\s*\(\s*["']([^"']*)["']\s*\)/);
  const basePath = classMapping ? classMapping[1] : '';

  const endpoints = [];
  // method-level mappings
  const methodRegex =
    /(?:@(Post|Put|Delete|Patch)Mapping)\s*(?:\(\s*(?:value\s*=\s*)?["']([^"']*)["']?[^)]*\)|\(\s*\))?/g;
  let m;
  while ((m = methodRegex.exec(content)) !== null) {
    const httpMethod = m[1].toUpperCase();
    const path = m[2] || '';
    const fullUrl = (basePath + '/' + path).replace(/\/+/g, '/').replace(/\/$/, '');
    endpoints.push({ method: httpMethod, url: fullUrl || '/' });
  }

  // check each endpoint for @InnerAuth nearby (simplified: if class has @InnerAuth on any method)
  // and @Idempotent presence
  const hasIdempotent = /@Idempotent/.test(content);

  return {
    file,
    pkg,
    basePath,
    endpoints,
    hasInnerAuth,
    hasIdempotent,
  };
}

function categorize() {
  const modules = readdirSync(modulesDir).filter((d) => {
    return statSync(join(modulesDir, d)).isDirectory() && d.startsWith('junsong-');
  });

  const result = {};
  let totalMutation = 0;
  let totalQuery = 0;
  let totalInternal = 0;

  for (const mod of modules) {
    const ctrlDir = join(modulesDir, mod, 'src', 'main', 'java');
    const files = findJavaFiles(ctrlDir, []);
    const controllers = files.map(parseController).filter(Boolean);

    for (const ctrl of controllers) {
      const ctrlName = ctrl.file.split('/').pop().replace('.java', '');
      const moduleKey = mod.replace('junsong-', '');

      for (const ep of ctrl.endpoints) {
        const fullUrl = '/' + moduleKey + ep.url;
        let category;

        // Check if endpoint method has @InnerAuth (simplified: check context around mapping)
        // For accuracy, we check if the controller file has @InnerAuth on the specific method
        const epContent = readFileSync(ctrl.file, 'utf8');
        const isInternal = hasInnerAuthOnMethod(epContent, ep);

        if (isInternal) {
          category = 'INTERNAL';
          totalInternal++;
        } else if (isQueryEndpoint(ep.url) || isQueryEndpoint(fullUrl)) {
          category = 'QUERY';
          totalQuery++;
        } else {
          category = 'MUTATION';
          totalMutation++;
        }

        if (!result[moduleKey]) result[moduleKey] = [];
        result[moduleKey].push({
          controller: ctrlName,
          file: ctrl.file,
          method: ep.method,
          url: ep.url,
          fullUrl,
          category,
          alreadyAnnotated: ctrl.hasIdempotent,
        });
      }
    }
  }

  return { result, totalMutation, totalQuery, totalInternal };
}

function hasInnerAuthOnMethod(content, endpoint) {
  // Simplified: find the mapping line and check if @InnerAuth is within 5 lines above
  const lines = content.split('\n');
  const methodPath = endpoint.url.split('/').pop();
  for (let i = 0; i < lines.length; i++) {
    if (lines[i].includes('@' + endpoint.method.charAt(0) + endpoint.method.slice(1).toLowerCase() + 'Mapping')) {
      // check next 5 lines above for @InnerAuth
      for (let j = Math.max(0, i - 5); j <= i; j++) {
        if (lines[j].includes('@InnerAuth')) return true;
      }
    }
  }
  return false;
}

const { result, totalMutation, totalQuery, totalInternal } = categorize();

console.log('========== 幂等端点分类报告 ==========');
console.log(`总端点数: ${totalMutation + totalQuery + totalInternal}`);
console.log(`MUTATION（需要 @Idempotent）: ${totalMutation}`);
console.log(`QUERY（跳过）: ${totalQuery}`);
console.log(`INTERNAL（跳过，@InnerAuth）: ${totalInternal}`);
console.log('');

for (const [mod, endpoints] of Object.entries(result).sort()) {
  const mutations = endpoints.filter((e) => e.category === 'MUTATION');
  const queries = endpoints.filter((e) => e.category === 'QUERY');
  const internals = endpoints.filter((e) => e.category === 'INTERNAL');
  const annotated = mutations.filter((e) => e.alreadyAnnotated);
  const pending = mutations.filter((e) => !e.alreadyAnnotated);

  if (mutations.length === 0) continue;

  console.log(`## ${mod} (${mutations.length} mutation, ${queries.length} query, ${internals.length} internal)`);
  console.log(`  已接入: ${annotated.length}, 待接入: ${pending.length}`);
  if (pending.length > 0) {
    for (const ep of pending) {
      console.log(`  [待接入] ${ep.controller}  ${ep.method.padEnd(6)} ${ep.url}`);
    }
  }
  console.log('');
}
