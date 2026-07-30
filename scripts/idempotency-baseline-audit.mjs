// 基线盘点脚本：扫描所有 Controller 的写端点，记录幂等治理状态。
// 用途：阶段 0 基线，阶段 7 全系统接入的对照清单。
// 运行：node --test scripts/idempotency-baseline-audit.mjs
// 或：node scripts/idempotency-baseline-audit.mjs

import { readFileSync, readdirSync, statSync, existsSync } from 'node:fs';
import { join, extname, basename, dirname } from 'node:path';
import { fileURLToPath } from 'node:url';
import { test, describe } from 'node:test';
import assert from 'node:assert/strict';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
const ROOT = join(__dirname, '..');

// Controller 文件清单（基于项目结构）
const CONTROLLER_DIRS = [
  'junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller',
  'junsong-modules/junsong-member/src/main/java/com/junsong/member/controller',
  'junsong-modules/junsong-system/src/main/java/com/junsong/system/controller',
  'junsong-modules/junsong-workflow/src/main/java/com/junsong/workflow/controller',
  'junsong-modules/junsong-open/src/main/java/com/junsong/open/controller',
  'junsong-modules/junsong-file/src/main/java/com/junsong/file/controller',
  'junsong-modules/junsong-job/src/main/java/com/junsong/job/controller',
];

function listJavaFiles(dir) {
  if (!existsSync(dir)) return [];
  const out = [];
  for (const name of readdirSync(dir)) {
    const full = join(dir, name);
    const st = statSync(full);
    if (st.isDirectory()) {
      out.push(...listJavaFiles(full));
    } else if (extname(name) === '.java') {
      out.push(full);
    }
  }
  return out;
}

function parseController(file) {
  const src = readFileSync(file, 'utf8');
  const className = basename(file, '.java');
  // 提取 @RequestMapping 类级路径
  let basePath = '';
  const m = src.match(/@RequestMapping\s*\(\s*"([^"]*)"\s*\)/);
  if (m) basePath = m[1];
  // 提取 @PostMapping / @PutMapping / @DeleteMapping 写端点
  const writes = [];
  const re = /@(PostMapping|PutMapping|DeleteMapping)\s*(?:\(\s*(?:"([^"]*)"|value\s*=\s*"([^"]*)")?\s*\))?/g;
  let match;
  while ((match = re.exec(src)) !== null) {
    const methodType = match[1];
    const path = match[2] || match[3] || '';
    writes.push({ methodType, path: basePath + path });
  }
  // 是否已接入 @Idempotent
  const hasIdempotent = /@Idempotent\s*\(/.test(src);
  // 是否有 X-Idempotency-Key
  const hasHeaderKey = /X-Idempotency-Key/.test(src);
  return { file, className, basePath, writes, hasIdempotent, hasHeaderKey };
}

function auditAll() {
  const report = [];
  for (const dir of CONTROLLER_DIRS) {
    const absDir = join(ROOT, dir);
    const files = listJavaFiles(absDir);
    for (const f of files) {
      const info = parseController(f);
      if (info.writes.length > 0) {
        report.push(info);
      }
    }
  }
  return report;
}

describe('幂等治理基线盘点', () => {
  test('盘点所有写端点并生成报告', () => {
    const report = auditAll();
    assert.ok(report.length > 0, '至少应扫描到 Controller');

    const totalWrites = report.reduce((s, r) => s + r.writes.length, 0);
    const withIdempotent = report.filter(r => r.hasIdempotent).length;
    const withHeaderKey = report.filter(r => r.hasHeaderKey).length;

    console.log('\n========== 幂等治理基线报告 ==========');
    console.log(`Controller 文件数: ${report.length}`);
    console.log(`写端点总数: ${totalWrites}`);
    console.log(`已接入 @Idempotent 的 Controller: ${withIdempotent}`);
    console.log(`已使用 X-Idempotency-Key 的 Controller: ${withHeaderKey}`);
    console.log('');

    for (const r of report) {
      const status = r.hasIdempotent ? '[已接入]' : '[待接入]';
      console.log(`${status} ${r.className} (${r.writes.length} 写端点) ${r.hasHeaderKey ? '🔑' : ''}`);
      for (const w of r.writes) {
        console.log(`    ${w.methodType.padEnd(12)} ${w.path}`);
      }
    }
    console.log('========================================\n');

    // 输出待接入清单
    const pending = report.filter(r => !r.hasIdempotent);
    console.log(`待接入 @Idempotent 的 Controller: ${pending.length}`);
    for (const r of pending) {
      console.log(`  - ${r.file.replace(ROOT + '/', '')}`);
    }

    // 断言：基线状态正确（阶段 0 应该有大量待接入）
    assert.ok(pending.length > 0, '阶段 0 应该有大量待接入的 Controller');
  });
});
