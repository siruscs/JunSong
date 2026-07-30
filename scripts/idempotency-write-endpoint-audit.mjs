#!/usr/bin/env node
/**
 * 全量写接口审计脚本（差集报告）。
 *
 * 扫描所有 Controller 中的写接口（@PostMapping/@PutMapping/@DeleteMapping/@PatchMapping），
 * 对每个接口检查是否有 @Idempotent 注解，输出：
 *   1. 全量写接口清单（含注解状态）
 *   2. 已加注解端点清单
 *   3. 未加注解端点清单（差集）
 *   4. 未加注解端点分类（按推断原因：导出/查询/批量删除/写库等）
 *   5. 模块级覆盖率统计
 *
 * 用法：node scripts/idempotency-write-endpoint-audit.mjs [输出文件路径]
 *      输出 Markdown 到 stdout，可重定向到文件。
 *
 * 与 idempotency-coverage-audit.mjs 的区别：
 *   - coverage-audit 只扫描已加注解的方法（无法发现未覆盖的写接口）
 *   - 本脚本扫描所有写接口，能发现未覆盖的写接口（差集）
 */

import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative, sep } from 'node:path';

const ROOT = process.argv[2] || process.cwd();

// Controller 文件所在目录（与 coverage-audit 保持一致）
const CONTROLLER_DIRS = [
    join(ROOT, 'junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller'),
    join(ROOT, 'junsong-modules/junsong-member/src/main/java/com/junsong/member/controller'),
    join(ROOT, 'junsong-modules/junsong-system/src/main/java/com/junsong/system/controller'),
    join(ROOT, 'junsong-modules/junsong-workflow/src/main/java/com/junsong/workflow/controller'),
    join(ROOT, 'junsong-modules/junsong-open/src/main/java/com/junsong/open/controller'),
    join(ROOT, 'junsong-modules/junsong-file/src/main/java/com/junsong/file/controller'),
    join(ROOT, 'junsong-modules/junsong-job/src/main/java/com/junsong/job/controller'),
];

// 写接口的 HTTP 方法注解
const WRITE_MAPPING_PATTERNS = [
    { regex: /@PostMapping\s*\(\s*(?:value\s*=\s*)?["']([^"']+)["']/, method: 'POST' },
    { regex: /@PostMapping\s*(?:\(\s*\))?\s*$/, method: 'POST', path: '' },
    { regex: /@PutMapping\s*\(\s*(?:value\s*=\s*)?["']([^"']+)["']/, method: 'PUT' },
    { regex: /@PutMapping\s*(?:\(\s*\))?\s*$/, method: 'PUT', path: '' },
    { regex: /@DeleteMapping\s*\(\s*(?:value\s*=\s*)?["']([^"']+)["']/, method: 'DELETE' },
    { regex: /@DeleteMapping\s*(?:\(\s*\))?\s*$/, method: 'DELETE', path: '' },
    { regex: /@PatchMapping\s*\(\s*(?:value\s*=\s*)?["']([^"']+)["']/, method: 'PATCH' },
    { regex: /@PatchMapping\s*(?:\(\s*\))?\s*$/, method: 'PATCH', path: '' },
];

// 可豁免的接口路径关键词（不应加幂等键的场景）
const EXEMPT_PATTERNS = [
    /\/export$/i,           // 导出接口
    /\/import$/i,           // 导入接口（通常批量处理，幂等性由业务层保证）
    /\/list$/i,             // 列表查询（虽然是 POST 但只读）
    /\/query$/i,            // 查询接口
    /\/search$/i,           // 搜索接口
    /\/preview$/i,          // 预览接口
    /\/check/i,             // 检查类接口
    /\/validate/i,          // 校验类接口
    /\/exists/i,            // 存在性检查
    /\/count$/i,            // 计数接口
    /\/summary$/i,          // 汇总接口
    /\/stats?$/i,           // 统计接口
    /\/download$/i,         // 下载接口
    /\/upload$/i,           // 上传接口（文件幂等性由业务层保证）
    /\/realTime/i,          // 实时查询
    // 第三轮复核后补充：POST 查询/报表/仪表盘类（只读）
    // 注意：/snapshot、/health-tasks/generate、/governance/action 是写库操作，不应豁免
    /\/dashboard/i,         // 仪表盘查询（但 /dashboard/governance/action 是写库，已加 @Idempotent）
    /\/report/i,            // 报表查询
    /\/board$/i,            // 看板查询
    /\/metrics$/i,          // 指标查询
    /\/drilldown/i,         // 钻取查询
    /\/what-if/i,           // 假设分析查询
    /\/candidates/i,        // 候选列表查询
    /\/effect$/i,           // 效果分析查询
    /\/portfolio/i,         // 组合查询
    /\/health-trend/i,      // 健康趋势查询
    /\/health-tasks\/list/i, // 健康任务列表查询（精确匹配，不豁免 /generate）
    /\/action-center/i,     // 行动中心查询
    // 标记已读/轻量操作（幂等性由业务层保证，重复操作无副作用）
    /\/markRead/i,          // 标记已读
    /\/markAllRead/i,       // 全部标记已读
    /\/read-all/i,          // 全部已读
    /\/read\//i,            // 通知已读
    /\/touch/i,             // 轻量触摸操作
    // 生成类（无副作用的令牌/邀请码生成）
    /\/generateToken/i,     // 令牌生成
    /\/generateInviteCode/i, // 邀请码生成
    /\/importTemplate/i,    // 导入模板下载
    // 日志/审计/告警内部接口（由调用方保证幂等）
    /\/internal\/log/i,     // 内部日志
    /\/internal\/batch-send/i, // 内部批量发送
    /\/internal\/raise/i,   // 内部告警
    /\/internal\/record/i,  // 内部审计记录
    // 登录相关低风险操作
    /\/recordlogin/i,       // 记录登录
    /\/login-time/i,        // 更新最后登录时间
    // 企业硬化查询类（只豁免查询，不豁免状态变更/归档操作）
    /\/hardening\/audits$/i, // 审计查询（精确匹配，避免误豁免子路径）
    /\/hardening\/alerts$/i, // 告警查询（精确匹配，避免误豁免 ack/resolve 子路径）
    // 周报/运营指标查询
    /\/weekly-board/i,      // 周报看板查询
    /\/weekly-memo/i,       // 周报复忘查询
    /\/operatingMetrics/i,  // 运营指标查询
    // 内部调度器接口（幂等由调度器保证）
    /\/inner\/scheduler\//i, // 内部调度器
    /\/inner\/predictive-ops\//i, // 内部预测操作
    // MP 绑定撤销（低风险）
    /\/mp-binding\/revoke/i, // 撤销绑定
    // 生成类（无副作用的令牌/邀请码/低代码生成）
    /\/invite-code\/generate/i, // 邀请码生成
    /\/lowcode-generate/i, // 低代码菜单生成
];

function listJavaFiles(dir) {
    const result = [];
    if (!exists(dir)) return result;
    for (const name of readdirSync(dir)) {
        const path = join(dir, name);
        const stat = statSync(path);
        if (stat.isDirectory()) {
            result.push(...listJavaFiles(path));
        } else if (name.endsWith('.java')) {
            result.push(path);
        }
    }
    return result;
}

function exists(p) {
    try { statSync(p); return true; } catch { return false; }
}

function extractClassName(content) {
    const match = content.match(/class\s+(\w+)/);
    return match ? match[1] : '(未知)';
}

function extractClassMapping(content) {
    const match = content.match(/@RequestMapping\s*\(\s*(?:value\s*=\s*)?["']([^"']+)["']/);
    return match ? match[1] : '';
}

function normalizePath(classMapping, methodPath) {
    if (!classMapping && !methodPath) return '';
    if (!classMapping) return methodPath || '';
    if (!methodPath) return classMapping;
    return classMapping.replace(/\/$/, '') + '/' + methodPath.replace(/^\//, '');
}

/**
 * 从方法上方的注解块中提取 @Idempotent 注解信息
 */
function extractIdempotentInfo(annotationBlock) {
    if (!annotationBlock.includes('@Idempotent')) return null;
    const scene = extractAttr(annotationBlock, 'scene');
    const highRisk = extractBool(annotationBlock, 'highRisk', false);
    const retryPolicy = extractAttr(annotationBlock, 'retryPolicy') || 'REQUIRE_NEW_KEY';
    const ttlSeconds = extractNumber(annotationBlock, 'ttlSeconds', 86400);
    return { scene, highRisk, retryPolicy, ttlSeconds };
}

function extractAttr(text, attr) {
    const re = new RegExp(`${attr}\\s*=\\s*["']([^"']+)["']`);
    const m = text.match(re);
    return m ? m[1] : null;
}

function extractBool(text, attr, defaultValue) {
    const re = new RegExp(`${attr}\\s*=\\s*(true|false)`);
    const m = text.match(re);
    return m ? m[1] === 'true' : defaultValue;
}

function extractNumber(text, attr, defaultValue) {
    const re = new RegExp(`${attr}\\s*=\\s*(\\d+)`);
    const m = text.match(re);
    return m ? parseInt(m[1], 10) : defaultValue;
}

/**
 * 推断未加注解接口的豁免原因
 */
function inferExemptReason(endpoint) {
    const path = endpoint.httpPath.toLowerCase();
    const method = endpoint.httpMethod;

    // 所有方法都检查豁免规则（POST 查询、PUT 标记已读、DELETE 清理等）
    for (const pattern of EXEMPT_PATTERNS) {
        if (pattern.test(path)) {
            return `可豁免（路径匹配：${pattern.source}）`;
        }
    }

    // POST 但路径是查询类的（额外检查方法名）
    if (method === 'POST') {
        // 已经被 EXEMPT_PATTERNS 覆盖，这里不需要额外检查
    }

    // DELETE 批量删除（路径含 /{ids} 或 /{xxxIds}）
    if (method === 'DELETE' && /\{[^}]*[Ii]ds?\}/.test(path)) {
        return '批量删除（业务层保证）';
    }

    // 上传/下载
    if (/\/upload|\/download/i.test(path)) {
        return '文件上传/下载（业务层保证）';
    }

    return null;
}

/**
 * 扫描一个 Controller 文件，提取所有写接口及其注解状态
 */
function extractWriteEndpoints(content, filePath) {
    const results = [];
    const className = extractClassName(content);
    const classMapping = extractClassMapping(content);
    const lines = content.split('\n');

    let i = 0;
    while (i < lines.length) {
        const line = lines[i];

        // 检测是否是写接口的 HTTP 映射注解
        let matchedMethod = null;
        let matchedPath = null;

        for (const pattern of WRITE_MAPPING_PATTERNS) {
            const m = line.match(pattern.regex);
            if (m) {
                matchedMethod = pattern.method;
                matchedPath = pattern.path !== undefined ? pattern.path : (m[1] || '');
                break;
            }
        }

        if (!matchedMethod) {
            i++;
            continue;
        }

        // 找到写接口，向后扫描方法签名
        let methodSignature = null;
        let methodLine = i;
        let j = i + 1;
        while (j < lines.length && j < i + 15) {
            const ml = lines[j];
            const methodMatch = ml.match(/public\s+\w+(?:<[^>]+>)?\s+(\w+)\s*\(/);
            if (methodMatch) {
                methodSignature = methodMatch[1];
                methodLine = j;
                break;
            }
            j++;
        }

        // 向前扫描注解块（从当前行向上找，直到遇到方法签名或类定义）
        // 注意：不能在空行处停止，因为 Java 代码中 @Idempotent 和 @PostMapping 之间可能有空行
        const annotationLines = [];
        let k = i - 1;
        while (k >= 0) {
            const al = lines[k].trim();
            // 遇到方法签名停止（上一个方法的结束）
            if (al.startsWith('public') || al.startsWith('private') || al.startsWith('protected')) break;
            // 遇到类/方法边界停止
            if (al === '}' || al === '{') break;
            // 遇到上一个方法的 HTTP 映射注解停止
            if (al.match(/@(PostMapping|PutMapping|DeleteMapping|PatchMapping|GetMapping|RequestMapping)\s*\(/)) break;
            annotationLines.unshift(al);
            k--;
        }
        // 同时向后扫描（从 HTTP 映射注解到方法签名之间），因为部分 Controller 的 @Idempotent 在 @PostMapping 之后
        if (methodLine > i) {
            for (let m = i + 1; m < methodLine; m++) {
                annotationLines.push(lines[m].trim());
            }
        }
        const annotationBlock = annotationLines.join('\n');

        // 检查是否有 @Idempotent
        const idempotentInfo = extractIdempotentInfo(annotationBlock);

        // 检查方法名是否含查询/导出关键词
        const fullPath = normalizePath(classMapping, matchedPath);

        // 检查是否有 @Log 注解（业务写操作的标志）
        const hasLogAnnotation = annotationBlock.includes('@Log(');

        const endpoint = {
            file: relative(ROOT, filePath),
            className,
            methodName: methodSignature || '(未识别)',
            httpMethod: matchedMethod,
            httpPath: fullPath || '(空)',
            hasIdempotent: !!idempotentInfo,
            idempotentInfo,
            hasLogAnnotation,
            annotationLine: i + 1,
            methodLine: methodLine + 1,
        };

        endpoint.exemptReason = inferExemptReason(endpoint);
        results.push(endpoint);

        i = j;
    }
    return results;
}

// ============================================================
// 主流程
// ============================================================

const allEndpoints = [];

for (const dir of CONTROLLER_DIRS) {
    const files = listJavaFiles(dir);
    for (const file of files) {
        const content = readFileSync(file, 'utf8');
        // 只处理含写接口注解的文件
        if (!content.includes('@PostMapping') && !content.includes('@PutMapping')
            && !content.includes('@DeleteMapping') && !content.includes('@PatchMapping')) {
            continue;
        }
        const endpoints = extractWriteEndpoints(content, file);
        allEndpoints.push(...endpoints);
    }
}

// 按模块归类
const moduleMap = {};
for (const ep of allEndpoints) {
    const moduleMatch = ep.file.match(/junsong-modules[\/\\]([^\/\\]+)/);
    const moduleName = moduleMatch ? moduleMatch[1] : '(其他)';
    ep.module = moduleName;
    if (!moduleMap[moduleName]) moduleMap[moduleName] = [];
    moduleMap[moduleName].push(ep);
}

// 分类
const withAnnotation = allEndpoints.filter(ep => ep.hasIdempotent);
const withoutAnnotation = allEndpoints.filter(ep => !ep.hasIdempotent);
const withoutAnnotationExempt = withoutAnnotation.filter(ep => ep.exemptReason !== null);
const withoutAnnotationNeedReview = withoutAnnotation.filter(ep => ep.exemptReason === null);

// ============================================================
// 输出 Markdown 报告
// ============================================================

console.log('# 全量写接口审计报告（差集分析）');
console.log('');
console.log(`> 生成时间：${new Date().toISOString()}`);
console.log(`> 扫描根目录：${ROOT}`);
console.log('');
console.log('## 1. 总体统计');
console.log('');
console.log('| 类别 | 数量 |');
console.log('|------|------|');
console.log(`| 全量写接口（POST/PUT/DELETE/PATCH） | ${allEndpoints.length} |`);
console.log(`| 已加 @Idempotent 注解 | ${withAnnotation.length} |`);
console.log(`| 未加 @Idempotent 注解 | ${withoutAnnotation.length} |`);
console.log(`| ├─ 可豁免（查询/导出/批量删除等） | ${withoutAnnotationExempt.length} |`);
console.log(`| └─ 需人工审查（疑似写库未覆盖） | ${withoutAnnotationNeedReview.length} |`);
const coverageRate = allEndpoints.length > 0
    ? ((withAnnotation.length / allEndpoints.length) * 100).toFixed(1)
    : '0.0';
console.log('');
console.log(`**注解覆盖率：${coverageRate}%**（已加注解 / 全量写接口）`);
console.log('');
console.log('> 注意：');
console.log('> - "已加注解端点数"不等于"全量写接口数"，本报告区分两者。');
console.log('> - "可豁免"包括 POST 查询/导出、批量删除、文件上传等场景。');
console.log('> - "需人工审查"是真正的差集，可能包含遗漏的写接口。');
console.log('> - 本脚本只扫描 Controller 层，不包含异步消费者、定时任务、工作流回调、Service 内部直接写库。');
console.log('');

console.log('## 2. 模块级覆盖率');
console.log('');
console.log('| 模块 | 全量写接口 | 已加注解 | 未加注解 | 可豁免 | 需审查 | 覆盖率 |');
console.log('|------|-----------|---------|---------|--------|--------|--------|');
for (const [moduleName, endpoints] of Object.entries(moduleMap).sort()) {
    const total = endpoints.length;
    const annotated = endpoints.filter(ep => ep.hasIdempotent).length;
    const unannotated = endpoints.filter(ep => !ep.hasIdempotent).length;
    const exempt = endpoints.filter(ep => !ep.hasIdempotent && ep.exemptReason !== null).length;
    const needReview = endpoints.filter(ep => !ep.hasIdempotent && ep.exemptReason === null).length;
    const rate = total > 0 ? ((annotated / total) * 100).toFixed(1) : '0.0';
    console.log(`| ${moduleName} | ${total} | ${annotated} | ${unannotated} | ${exempt} | ${needReview} | ${rate}% |`);
}
const totalAll = allEndpoints.length;
const annotatedAll = withAnnotation.length;
const rateAll = totalAll > 0 ? ((annotatedAll / totalAll) * 100).toFixed(1) : '0.0';
console.log(`| **合计** | **${totalAll}** | **${annotatedAll}** | **${withoutAnnotation.length}** | **${withoutAnnotationExempt.length}** | **${withoutAnnotationNeedReview.length}** | **${rateAll}%** |`);
console.log('');

console.log('## 3. 需人工审查的未加注解写接口（差集）');
console.log('');
console.log('> 以下是未加 @Idempotent 注解且不匹配可豁免规则的写接口，需逐个确认是否需要幂等保护。');
console.log('');
if (withoutAnnotationNeedReview.length === 0) {
    console.log('（无）');
} else {
    console.log('| 模块 | 文件 | 类 | 方法 | HTTP | 路径 | 有 @Log |');
    console.log('|------|------|-----|------|-------|------|---------|');
    for (const ep of withoutAnnotationNeedReview.sort((a, b) => a.module.localeCompare(b.module) || a.file.localeCompare(b.file))) {
        console.log(`| ${ep.module} | ${ep.file} | ${ep.className} | ${ep.methodName} | ${ep.httpMethod} | ${ep.httpPath} | ${ep.hasLogAnnotation ? '是' : '否'} |`);
    }
}
console.log('');

console.log('## 4. 可豁免的未加注解写接口（按豁免原因分组）');
console.log('');
if (withoutAnnotationExempt.length === 0) {
    console.log('（无）');
} else {
    const byReason = {};
    for (const ep of withoutAnnotationExempt) {
        if (!byReason[ep.exemptReason]) byReason[ep.exemptReason] = [];
        byReason[ep.exemptReason].push(ep);
    }
    for (const [reason, eps] of Object.entries(byReason).sort()) {
        console.log(`### ${reason}（${eps.length} 个）`);
        console.log('');
        console.log('| 模块 | 类 | 方法 | HTTP | 路径 |');
        console.log('|------|-----|------|-------|------|');
        for (const ep of eps.sort((a, b) => a.module.localeCompare(b.module))) {
            console.log(`| ${ep.module} | ${ep.className} | ${ep.methodName} | ${ep.httpMethod} | ${ep.httpPath} |`);
        }
        console.log('');
    }
}
console.log('');

console.log('## 5. 已加 @Idempotent 注解的写接口清单');
console.log('');
console.log('| 模块 | 类 | 方法 | HTTP | 路径 | scene | highRisk | retryPolicy |');
console.log('|------|-----|------|-------|------|-------|----------|-------------|');
for (const ep of withAnnotation.sort((a, b) => a.module.localeCompare(b.module) || a.file.localeCompare(b.file))) {
    const info = ep.idempotentInfo || {};
    console.log(`| ${ep.module} | ${ep.className} | ${ep.methodName} | ${ep.httpMethod} | ${ep.httpPath} | ${info.scene || ''} | ${info.highRisk ? '是' : '否'} | ${info.retryPolicy} |`);
}
console.log('');

console.log('## 6. 未覆盖范围说明');
console.log('');
console.log('本脚本基于 Controller 层静态扫描，**无法发现**以下写入口：');
console.log('');
console.log('| 入口类型 | 说明 | 建议 |');
console.log('|---------|------|------|');
console.log('| 异步消费者 | MQ/Kafka 消费者回调 | 在消费者入口处手动调用 IdempotencyRecordService.acquire |');
console.log('| 定时任务 | @Scheduled 注解方法 | 由业务状态机保证幂等（如期间状态、批次状态） |');
console.log('| 工作流回调 | Flowable/Activiti 回调 | 在回调 Controller 加 @Idempotent 或业务状态机兜底 |');
console.log('| Service 内部直接写库 | 非 Controller 入口的 Service 方法 | 由调用方 Controller 的 @Idempotent 覆盖 |');
console.log('| 远程调用入口 | Feign/RPC 调用 | 由调用方 Controller 的 @Idempotent 覆盖 |');
console.log('| 动态路由 | 反射/动态代理 | 需在入口处显式加注解或调用 acquire |');
console.log('');
console.log('对于上述入口，建议在 DEV 部署后通过 APM/日志审计验证。');
