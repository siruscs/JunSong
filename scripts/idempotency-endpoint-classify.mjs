#!/usr/bin/env node
/**
 * 写接口分类审计脚本（第三轮复核要求 2：逐项审查 176 个写接口）
 *
 * 对 idempotency-write-endpoint-audit.mjs 输出的"需人工审查"接口逐项分类：
 *   A. 可豁免-POST查询类：路径含 dashboard/report/board/snapshot/metrics/preview 等查询关键词
 *   B. 可豁免-导入模板下载：路径含 importTemplate
 *   C. 可豁免-内部接口：路径含 /inner/ 或 /internal/ 的服务间调用
 *   D. 需加注解-CRUD写操作：有 @Log 注解或方法名为 add/edit/remove/submit/withdraw
 *   E. 需加注解-状态变更：方法名含 approve/reject/activate/suspend/terminate/claim/complete/reopen
 *   F. 需加注解-开放API：/open/ 路径下对外暴露的写接口
 *   G. 需评估-工作流回调：方法名含 syncWorkflowStatus
 *   H. 需评估-其他：需逐个确认
 */

import { readFileSync } from 'node:fs';
import { join } from 'node:path';

const ROOT = process.argv[2] || process.cwd();

// 读取审计报告
const reportPath = join(ROOT, 'docs/superpowers/plans/2026-07-27-idempotency-write-endpoint-audit-report.zh-CN.md');
const content = readFileSync(reportPath, 'utf8');

// 解析"需人工审查"表格
const lines = content.split('\n');
const endpoints = [];
let inNeedReviewSection = false;

for (const line of lines) {
    if (line.includes('## 3. 需人工审查')) {
        inNeedReviewSection = true;
        continue;
    }
    if (line.includes('## 4. ')) {
        inNeedReviewSection = false;
        continue;
    }
    if (!inNeedReviewSection) continue;
    if (!line.startsWith('| junsong-')) continue;
    if (line.includes('---|')) continue;

    const parts = line.split('|').map(p => p.trim()).filter(p => p);
    if (parts.length < 7) continue;

    const [module, file, className, method, http, path, hasLog] = parts;
    endpoints.push({
        module,
        file,
        className,
        method: method || '(未识别)',
        httpMethod: http,
        path,
        hasLog: hasLog === '是',
    });
}

// 分类规则
function classify(ep) {
    const path = ep.path.toLowerCase();
    const method = ep.method.toLowerCase();
    const className = ep.className.toLowerCase();

    // A. 可豁免-POST查询类
    if (ep.httpMethod === 'POST') {
        if (/\/dashboard|\/board|\/report|\/snapshot|\/metrics|\/preview|\/candidates|\/effect|\/what-if|\/drilldown|\/portfolio|\/health-trend|\/health-tasks|\/action-center/.test(path)) {
            return { category: 'A', recommendation: '可豁免', reason: 'POST 查询/仪表盘/报表类（只读）' };
        }
        if (/\/generate$/.test(path) && !ep.hasLog) {
            // generate 可能是报表生成也可能是数据生成，需看类名
            if (/dashboard|report|growth-action/.test(className)) {
                return { category: 'A', recommendation: '可豁免', reason: 'POST 查询/报表生成类（只读）' };
            }
        }
    }

    // B. 可豁免-导入模板下载
    if (/importtemplate/i.test(path) || method === 'importtemplate') {
        return { category: 'B', recommendation: '可豁免', reason: '导入模板下载（只读）' };
    }

    // C. 可豁免-内部接口（服务间调用）
    if (/\/inner\/|\/internal\//.test(path)) {
        // 但要区分：内部调度器触发写库 vs 内部查询
        if (/scheduler|cashflow-snapshot|stock-snapshot|memo-draft|growth-effect-backfill|action-predictions/.test(path)) {
            return { category: 'C', recommendation: '需评估', reason: '内部调度器接口（非 Controller 入口，幂等由调度器保证）' };
        }
        if (/\/internal\/log|\/internal\/record|\/internal\/raise|\/internal\/batch-send/.test(path)) {
            return { category: 'C', recommendation: '需评估', reason: '内部接口（服务间调用，由调用方保证幂等）' };
        }
        return { category: 'C', recommendation: '需评估', reason: '内部接口（服务间调用）' };
    }

    // G. 需评估-工作流回调
    if (/syncworkflowstatus|workflow\/sync/.test(method + path)) {
        return { category: 'G', recommendation: '需加注解', reason: '工作流回调写库接口（应加 @Idempotent + ALLOW_SAME_KEY）' };
    }

    // E. 需加注解-状态变更/审批类
    if (/^(approve|reject|activate|suspend|terminate|claim|complete|reopen|changeStatus|toggle|jump|withdraw|submit|markRead|markReadAll|markAllRead|resetPwd|insertAuthRole|switchDept|forceLogout|revokeAll|revoke|generateToken|testEvent|refreshCache|clean|touch|trigger|register|recordlogin|updateLastLoginTime)$/.test(method)) {
        if (ep.hasLog) {
            return { category: 'E', recommendation: '需加注解', reason: `状态变更/审批类写操作（${method}，有 @Log）` };
        }
        // 无 @Log 但方法名明确是状态变更
        if (/^(approve|reject|activate|suspend|terminate|claim|complete|reopen|changeStatus|toggle|withdraw|submit)$/.test(method)) {
            return { category: 'E', recommendation: '需加注解', reason: `状态变更/审批类写操作（${method}）` };
        }
        // 无 @Log 的标记已读/清理等操作
        if (/^(markRead|markReadAll|markAllRead|clean|refreshCache)$/.test(method)) {
            return { category: 'E', recommendation: '可豁免', reason: `${method} 操作（批量标记/清理，低风险）` };
        }
    }

    // F. 需加注解-开放API写接口
    if (className.startsWith('open') && !className.includes('internal')) {
        if (/^(createPointsRecord|createPointsExchange|addStoreOpening|submitStoreOpening|withdrawStoreOpening|createWebhookSubscription|startInstance|approveTask|rejectTask|transferTask|register|edit|approve|reject|activate)$/.test(method)) {
            return { category: 'F', recommendation: '需加注解', reason: '开放 API 写接口（第三方调用，必须幂等）' };
        }
    }

    // D. 需加注解-CRUD写操作
    if (ep.hasLog || /^(add|edit|remove|insert|update|hire|leave|batchAdd|importData|signIn|backfill|awardSaleGrowth|generate)$/.test(method)) {
        if (ep.hasLog) {
            return { category: 'D', recommendation: '需加注解', reason: `CRUD 写操作（${method}，有 @Log）` };
        }
        // 无 @Log 但方法名是 CRUD
        if (/^(add|edit|insert|update|hire|leave|batchAdd)$/.test(method)) {
            return { category: 'D', recommendation: '需加注解', reason: `CRUD 写操作（${method}）` };
        }
        // 签到/补签/成长值发放
        if (/^(signIn|backfill|awardSaleGrowth)$/.test(method)) {
            return { category: 'D', recommendation: '需加注解', reason: `业务写操作（${method}）` };
        }
        // generate（非报表类）
        if (method === 'generate' && !/dashboard|report/.test(className)) {
            return { category: 'D', recommendation: '需加注解', reason: `数据生成写操作（${method}）` };
        }
    }

    // 兜底分类
    if (ep.hasLog) {
        return { category: 'D', recommendation: '需加注解', reason: `写操作（有 @Log）` };
    }

    return { category: 'H', recommendation: '需评估', reason: '其他（需逐个确认）' };
}

// 对每个端点进行分类
const classified = endpoints.map(ep => {
    const result = classify(ep);
    return { ...ep, ...result };
});

// 按分类分组
const groups = {};
for (const ep of classified) {
    if (!groups[ep.category]) groups[ep.category] = [];
    groups[ep.category].push(ep);
}

// 统计
const stats = {
    'A': { name: '可豁免-POST查询类', count: 0 },
    'B': { name: '可豁免-导入模板下载', count: 0 },
    'C': { name: '需评估-内部接口', count: 0 },
    'D': { name: '需加注解-CRUD写操作', count: 0 },
    'E': { name: '需加注解-状态变更', count: 0 },
    'F': { name: '需加注解-开放API', count: 0 },
    'G': { name: '需加注解-工作流回调', count: 0 },
    'H': { name: '需评估-其他', count: 0 },
};

for (const ep of classified) {
    stats[ep.category].count++;
}

// 输出报告
console.log('# 写接口分类审计报告（176 个需审查接口逐项分类）');
console.log('');
console.log('> 生成时间：' + new Date().toISOString());
console.log('> 审计目标：对 idempotency-write-endpoint-audit.mjs 输出的 176 个"需人工审查"接口逐项分类');
console.log('');

console.log('## 1. 分类统计');
console.log('');
console.log('| 分类 | 类别名称 | 数量 | 建议 |');
console.log('|------|----------|------|------|');
let exemptCount = 0, needAnnotationCount = 0, needEvalCount = 0;
for (const key of ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H']) {
    if (stats[key].count === 0) continue;
    const rec = key === 'A' || key === 'B' ? '可豁免' : (key === 'C' || key === 'H' ? '需评估' : '需加注解');
    if (rec === '可豁免') exemptCount += stats[key].count;
    else if (rec === '需加注解') needAnnotationCount += stats[key].count;
    else needEvalCount += stats[key].count;
    console.log(`| ${key} | ${stats[key].name} | ${stats[key].count} | ${rec} |`);
}
console.log(`| **合计** | — | **${classified.length}** | — |`);
console.log('');
console.log(`> 分类结论：`);
console.log(`> - 可豁免：${exemptCount} 个（POST 查询类 + 导入模板下载）`);
console.log(`> - 需加注解：${needAnnotationCount} 个（CRUD 写操作 + 状态变更 + 开放 API + 工作流回调）`);
console.log(`> - 需评估：${needEvalCount} 个（内部接口 + 其他）`);
console.log('');

console.log('## 2. 需加注解的写接口清单（需添加 @Idempotent）');
console.log('');
for (const key of ['D', 'E', 'F', 'G']) {
    const items = groups[key] || [];
    if (items.length === 0) continue;
    console.log(`### ${stats[key].name}（${items.length} 个）`);
    console.log('');
    console.log('| 模块 | 类 | 方法 | HTTP | 路径 | 有 @Log | 分类原因 |');
    console.log('|------|-----|------|-------|------|---------|----------|');
    for (const ep of items) {
        console.log(`| ${ep.module} | ${ep.className} | ${ep.method} | ${ep.httpMethod} | ${ep.path} | ${ep.hasLog ? '是' : '否'} | ${ep.reason} |`);
    }
    console.log('');
}

console.log('## 3. 可豁免的写接口清单');
console.log('');
for (const key of ['A', 'B']) {
    const items = groups[key] || [];
    if (items.length === 0) continue;
    console.log(`### ${stats[key].name}（${items.length} 个）`);
    console.log('');
    console.log('| 模块 | 类 | 方法 | HTTP | 路径 | 豁免原因 |');
    console.log('|------|-----|------|-------|------|----------|');
    for (const ep of items) {
        console.log(`| ${ep.module} | ${ep.className} | ${ep.method} | ${ep.httpMethod} | ${ep.path} | ${ep.reason} |`);
    }
    console.log('');
}

console.log('## 4. 需评估的写接口清单');
console.log('');
for (const key of ['C', 'H']) {
    const items = groups[key] || [];
    if (items.length === 0) continue;
    console.log(`### ${stats[key].name}（${items.length} 个）`);
    console.log('');
    console.log('| 模块 | 类 | 方法 | HTTP | 路径 | 评估原因 |');
    console.log('|------|-----|------|-------|------|----------|');
    for (const ep of items) {
        console.log(`| ${ep.module} | ${ep.className} | ${ep.method} | ${ep.httpMethod} | ${ep.path} | ${ep.reason} |`);
    }
    console.log('');
}
