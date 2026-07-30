#!/usr/bin/env node
/**
 * 全量写接口审计脚本（差集分析）。
 *
 * 扫描所有 Controller 中的写映射注解（@PostMapping / @PutMapping / @DeleteMapping / @PatchMapping，
 * 以及 @RequestMapping(method = RequestMethod.POST/PUT/DELETE/PATCH)），与 @Idempotent 注解做差集，
 * 输出"已加注解端点 vs 未加注解端点"的 Markdown 报告。
 *
 * 与 scripts/idempotency-coverage-audit.mjs 的差异：
 * - 旧脚本只扫描带 @Idempotent 的方法，无法发现未加注解的写接口；
 * - 本脚本以"全量写接口"为基准，输出未覆盖端点清单，并推测可能排除原因。
 *
 * 用法：node scripts/idempotency-write-endpoint-gap-audit.mjs
 */

import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative, sep } from 'node:path';

const ROOT = process.argv[2] || process.cwd();

// Controller 文件所在目录（按任务要求，只扫描这些目录）
const CONTROLLER_DIRS = [
    join(ROOT, 'junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller'),
    join(ROOT, 'junsong-modules/junsong-member/src/main/java/com/junsong/member/controller'),
    join(ROOT, 'junsong-modules/junsong-system/src/main/java/com/junsong/system/controller'),
    join(ROOT, 'junsong-modules/junsong-workflow/src/main/java/com/junsong/workflow/controller'),
    join(ROOT, 'junsong-modules/junsong-open/src/main/java/com/junsong/open/controller'),
    join(ROOT, 'junsong-modules/junsong-file/src/main/java/com/junsong/file/controller'),
    join(ROOT, 'junsong-modules/junsong-job/src/main/java/com/junsong/job/controller'),
];

// ============================================================
// 工具函数
// ============================================================

function exists(p) {
    try { statSync(p); return true; } catch { return false; }
}

/**
 * 递归列出所有 .java 文件
 */
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

/**
 * 移除 Java 注释（块注释 /* ... *\/ 与行注释 //）。
 * 同时正确处理字符串字面量与字符字面量，避免误删字符串中的注释标记。
 */
function stripComments(content) {
    let out = '';
    let i = 0;
    let inString = false;
    let inChar = false;
    let inBlockComment = false;
    let inLineComment = false;
    let escaped = false;

    while (i < content.length) {
        const c = content[i];
        const next = content[i + 1];

        if (inBlockComment) {
            if (c === '*' && next === '/') {
                inBlockComment = false;
                i += 2;
                continue;
            }
            i++;
            continue;
        }
        if (inLineComment) {
            if (c === '\n') {
                inLineComment = false;
                out += c;
            }
            i++;
            continue;
        }
        if (inString) {
            if (escaped) {
                escaped = false;
                out += c;
                i++;
                continue;
            }
            if (c === '\\') {
                escaped = true;
                out += c;
                i++;
                continue;
            }
            if (c === '"') {
                inString = false;
            }
            out += c;
            i++;
            continue;
        }
        if (inChar) {
            if (escaped) {
                escaped = false;
                out += c;
                i++;
                continue;
            }
            if (c === '\\') {
                escaped = true;
                out += c;
                i++;
                continue;
            }
            if (c === "'") {
                inChar = false;
            }
            out += c;
            i++;
            continue;
        }
        // 不在任何字符串/字符/注释中
        if (c === '/' && next === '*') {
            inBlockComment = true;
            i += 2;
            continue;
        }
        if (c === '/' && next === '/') {
            inLineComment = true;
            i += 2;
            continue;
        }
        if (c === '"') {
            inString = true;
            out += c;
            i++;
            continue;
        }
        if (c === "'") {
            inChar = true;
            out += c;
            i++;
            continue;
        }
        out += c;
        i++;
    }
    return out;
}

/**
 * 提取类名
 */
function extractClassName(content) {
    const m = content.match(/\bclass\s+(\w+)/);
    return m ? m[1] : '(未知)';
}

/**
 * 提取类级 @RequestMapping 前缀路径。
 * 只在 class 关键字之前的注解区查找，避免误匹配方法级 @RequestMapping。
 * 支持单路径与数组路径（取首个），如：
 *   @RequestMapping("/foo")
 *   @RequestMapping(value = "/foo")
 *   @RequestMapping({"/foo", "/bar"})  → "/foo"
 */
function extractClassMapping(content) {
    const classIdx = content.search(/\b(?:public\s+)?class\s+\w+/);
    if (classIdx < 0) return '';
    const before = content.slice(0, classIdx);
    // 匹配 @RequestMapping 后第一个字符串字面量（兼容 value= 与数组形式）
    const m = before.match(/@RequestMapping\s*\(\s*(?:value\s*=\s*)?(?:\{)?\s*["']([^"']+)["']/);
    return m ? m[1] : '';
}

/**
 * 路径拼接：类级路径 + 方法级路径，规范化重复斜杠。
 */
function normalizePath(classMapping, methodPath) {
    const left = (classMapping || '').replace(/\/$/, '');
    const right = (methodPath || '').replace(/^\//, '');
    if (!left && !right) return '/';
    if (!left) return '/' + right;
    if (!right) return left;
    return left + '/' + right;
}

/**
 * 匹配写映射注解行。
 * 返回 { method: 'POST'|'PUT'|'DELETE'|'PATCH' } 或 null。
 * 支持 @PostMapping / @PutMapping / @DeleteMapping / @PatchMapping
 * 也兼容方法级 @RequestMapping(method = RequestMethod.POST) 等形式。
 */
function matchWriteMapping(line) {
    if (/^\s*@PostMapping\b/.test(line)) return { method: 'POST' };
    if (/^\s*@PutMapping\b/.test(line)) return { method: 'PUT' };
    if (/^\s*@DeleteMapping\b/.test(line)) return { method: 'DELETE' };
    if (/^\s*@PatchMapping\b/.test(line)) return { method: 'PATCH' };
    // 方法级 @RequestMapping(method = RequestMethod.POST/PUT/DELETE/PATCH)
    // 注意：类级 @RequestMapping 不带 method 参数，不会误匹配
    const m = line.match(/@RequestMapping\s*\([^)]*\bmethod\s*=\s*RequestMethod\.(POST|PUT|DELETE|PATCH)\b/);
    if (m) return { method: m[1] };
    return null;
}

/**
 * 从一段注解文本中提取字符串属性
 */
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
 * 提取一个 Controller 文件中所有的写端点。
 * 对每个写端点，记录：
 *   - className, methodName, httpMethod, httpPath
 *   - hasIdempotent（方法注解块是否含 @Idempotent）
 *   - idempotentText（@Idempotent 注解文本，用于解析属性）
 */
function extractWriteEndpoints(content, filePath) {
    const results = [];
    const className = extractClassName(content);
    const classMapping = extractClassMapping(content);
    const lines = content.split('\n');

    let i = 0;
    while (i < lines.length) {
        const line = lines[i];
        const mapping = matchWriteMapping(line);
        if (!mapping) {
            i++;
            continue;
        }

        // 收集完整映射注解文本（注解可能跨多行，如 @PostMapping(\n  "/path"\n)）
        let fullAnnotationText = line;
        let depth = (line.match(/\(/g) || []).length - (line.match(/\)/g) || []).length;
        let j = i;
        while (depth > 0 && j + 1 < lines.length) {
            j++;
            fullAnnotationText += '\n' + lines[j];
            depth += (lines[j].match(/\(/g) || []).length;
            depth -= (lines[j].match(/\)/g) || []).length;
        }

        // 提取方法级路径（优先 value= / path=，否则取首个字符串字面量）
        let methodPath = '';
        const pathMatch =
            fullAnnotationText.match(/(?:value|path)\s*=\s*["']([^"']+)["']/) ||
            fullAnnotationText.match(/["']([^"']+)["']/);
        if (pathMatch) methodPath = pathMatch[1];

        const fullPath = normalizePath(classMapping, methodPath);

        // 寻找方法签名（可能跨行；从映射注解所在行开始向后扫描）
        // 同时收集映射注解与方法签名之间的注解行（@Idempotent 可能出现在 @PostMapping 之后）
        let methodName = '(未识别)';
        let methodSigLine = -1;
        let k = i;
        while (k < lines.length && k < i + 30) {
            const methodLine = lines[k];
            // 匹配 public/protected/private 返回类型 方法名(
            // 返回类型字符类需包含 . 以支持 R<ExpenseOcrService.OcrResult> 等带包名的泛型
            const sigMatch = methodLine.match(
                /(?:public|protected|private)\s+(?:[\w<>,\s\[\]?.]+?)\s+(\w+)\s*\(/
            );
            if (sigMatch) {
                methodName = sigMatch[1];
                methodSigLine = k;
                break;
            }
            k++;
        }

        // 收集方法的完整注解块：
        // 1) 向上扫描（@Idempotent 通常在 @PostMapping 之上）
        // 2) 向下扫描映射注解与方法签名之间的注解（@Idempotent 也可能在 @PostMapping 之下）
        let annotationBlock = '';

        // 向上扫描
        let backIdx = i - 1;
        while (backIdx >= 0) {
            const backLine = lines[backIdx].trim();
            if (backLine === '') {
                backIdx--;
                continue;
            }
            if (backLine.startsWith('@')) {
                annotationBlock = backLine + '\n' + annotationBlock;
                backIdx--;
            } else {
                break;
            }
        }

        // 向下扫描（从映射注解结束行 j+1 到方法签名行 methodSigLine-1）
        if (methodSigLine > 0) {
            for (let downIdx = j + 1; downIdx < methodSigLine; downIdx++) {
                const downLine = lines[downIdx].trim();
                if (downLine === '') continue;
                if (downLine.startsWith('@')) {
                    annotationBlock += downLine + '\n';
                }
            }
        }

        const hasIdempotent = /@Idempotent\b/.test(annotationBlock);
        // 从注解块中提取 @Idempotent 的完整文本（单行形式）
        const idempotentText = hasIdempotent
            ? annotationBlock.split('\n').find(l => l.startsWith('@Idempotent')) || ''
            : '';

        results.push({
            file: relative(ROOT, filePath),
            className,
            methodName,
            httpMethod: mapping.method,
            httpPath: fullPath,
            methodPath,
            hasIdempotent,
            idempotentText,
            annotationLine: i + 1,
        });

        i = j + 1;
    }
    return results;
}

/**
 * 推测未加注解端点的可能排除原因。
 */
function inferExclusionReason(endpoint) {
    const path = (endpoint.httpPath || '').toLowerCase();
    const methodName = (endpoint.methodName || '').toLowerCase();
    const className = (endpoint.className || '');

    // 内部接口（路径含 /internal/ 或 /inner/，或类名含 Inner）
    if (path.includes('/internal/') || path.includes('/inner/')) {
        return '内部接口（路径含 /internal/ 或 /inner/）';
    }
    if (/Inner/i.test(className)) {
        return '内部接口（Controller 类名含 Inner）';
    }

    // 查询/导出/下载类路径
    const queryPathKeywords = ['/list', '/query', '/search', '/export', '/import', '/template', '/download'];
    for (const kw of queryPathKeywords) {
        if (path.includes(kw)) {
            return `查询/导出类（路径含 ${kw}）`;
        }
    }

    // 方法名前缀
    const methodNamePrefixes = ['get', 'list', 'query', 'search', 'export', 'download', 'page'];
    for (const prefix of methodNamePrefixes) {
        if (methodName.startsWith(prefix)) {
            return `查询类（方法名以 ${prefix} 开头）`;
        }
    }

    return '需人工评估';
}

// ============================================================
// 主流程
// ============================================================

const allEndpoints = [];

for (const dir of CONTROLLER_DIRS) {
    const files = listJavaFiles(dir);
    const moduleName = relative(join(ROOT, 'junsong-modules'), dir).split(sep)[0];

    for (const file of files) {
        const raw = readFileSync(file, 'utf8');
        const content = stripComments(raw);
        // 仅处理 Controller 类（避免误扫 dto 等普通 Java 类）
        // 注意：@ 前不能用 \b（@ 是非单词字符，行首/空格后不是单词边界）
        if (!/@(RestController|Controller)\b/.test(content)) continue;
        const endpoints = extractWriteEndpoints(content, file);
        for (const ep of endpoints) {
            ep.module = moduleName;
            allEndpoints.push(ep);
        }
    }
}

// 统计
const total = allEndpoints.length;
const annotated = allEndpoints.filter(e => e.hasIdempotent);
const unannotated = allEndpoints.filter(e => !e.hasIdempotent);
const coverageRate = total > 0 ? (annotated.length / total * 100).toFixed(2) : '0.00';

// 按模块统计
const moduleStats = {};
for (const ep of allEndpoints) {
    if (!moduleStats[ep.module]) {
        moduleStats[ep.module] = { total: 0, annotated: 0, unannotated: 0 };
    }
    moduleStats[ep.module].total++;
    if (ep.hasIdempotent) {
        moduleStats[ep.module].annotated++;
    } else {
        moduleStats[ep.module].unannotated++;
    }
}

// ============================================================
// 输出 Markdown 报告
// ============================================================

const out = [];
out.push('# 全量写接口审计报告（差集分析）');
out.push('');
out.push(`> 生成时间：${new Date().toISOString()}`);
out.push(`> 扫描根目录：${ROOT}`);
out.push(`> 扫描目录：junsong-modules/{finance,member,system,workflow,open,file,job}/.../controller/`);
out.push('');

// 摘要
out.push('## 摘要');
out.push('');
out.push('| 指标 | 数量 |');
out.push('|------|------|');
out.push(`| 写端点总数 | ${total} |`);
out.push(`| 已加 @Idempotent 注解 | ${annotated.length} |`);
out.push(`| 未加注解 | ${unannotated.length} |`);
out.push(`| 覆盖率 | ${coverageRate}% |`);
out.push('');

// 按模块统计
out.push('## 按模块统计');
out.push('');
out.push('| 模块 | 写端点总数 | 已加注解 | 未加注解 | 覆盖率 |');
out.push('|------|-----------|---------|---------|--------|');
for (const [m, s] of Object.entries(moduleStats).sort()) {
    const rate = s.total > 0 ? (s.annotated / s.total * 100).toFixed(2) : '0.00';
    out.push(`| ${m} | ${s.total} | ${s.annotated} | ${s.unannotated} | ${rate}% |`);
}
out.push(`| **合计** | **${total}** | **${annotated.length}** | **${unannotated.length}** | **${coverageRate}%** |`);
out.push('');

// 未加注解端点清单
out.push('## 未加注解端点清单');
out.push('');
const unannotatedByModule = {};
for (const ep of unannotated) {
    if (!unannotatedByModule[ep.module]) unannotatedByModule[ep.module] = [];
    unannotatedByModule[ep.module].push(ep);
}

const unannotatedModuleNames = Object.keys(unannotatedByModule).sort();
if (unannotatedModuleNames.length === 0) {
    out.push('_所有写端点均已加 @Idempotent 注解。_');
    out.push('');
} else {
    for (const moduleName of unannotatedModuleNames) {
        const eps = unannotatedByModule[moduleName].sort(
            (a, b) => a.className.localeCompare(b.className) || a.methodName.localeCompare(b.methodName)
        );
        out.push(`### ${moduleName}`);
        out.push('');
        out.push('| Controller | 方法 | HTTP 方法 | HTTP 路径 | 推测排除原因 |');
        out.push('|------------|------|-----------|-----------|-------------|');
        for (const ep of eps) {
            const reason = inferExclusionReason(ep);
            out.push(`| ${ep.className} | ${ep.methodName} | ${ep.httpMethod} | \`${ep.httpPath}\` | ${reason} |`);
        }
        out.push('');
    }
}

// 已加注解端点清单（参考）
out.push('## 已加注解端点清单（参考）');
out.push('');
const annotatedByModule = {};
for (const ep of annotated) {
    if (!annotatedByModule[ep.module]) annotatedByModule[ep.module] = [];
    annotatedByModule[ep.module].push(ep);
}

const annotatedModuleNames = Object.keys(annotatedByModule).sort();
if (annotatedModuleNames.length === 0) {
    out.push('_未发现已加 @Idempotent 注解的写端点。_');
    out.push('');
} else {
    for (const moduleName of annotatedModuleNames) {
        const eps = annotatedByModule[moduleName].sort(
            (a, b) => a.className.localeCompare(b.className) || a.methodName.localeCompare(b.methodName)
        );
        out.push(`### ${moduleName}`);
        out.push('');
        out.push('| Controller | 方法 | HTTP 方法 | HTTP 路径 | 幂等场景 | highRisk | ttlSeconds |');
        out.push('|------------|------|-----------|-----------|----------|----------|------------|');
        for (const ep of eps) {
            const scene = extractAttr(ep.idempotentText, 'scene') || '';
            const highRisk = extractBool(ep.idempotentText, 'highRisk', false);
            const ttlSeconds = extractNumber(ep.idempotentText, 'ttlSeconds', 86400);
            out.push(`| ${ep.className} | ${ep.methodName} | ${ep.httpMethod} | \`${ep.httpPath}\` | ${scene} | ${highRisk} | ${ttlSeconds} |`);
        }
        out.push('');
    }
}

// 隐藏的 JSON 详情，便于后续处理
out.push('<!-- 详细数据（JSON）：');
out.push(JSON.stringify({ total, annotated: annotated.length, unannotated: unannotated.length, endpoints: allEndpoints }, null, 2));
out.push('-->');

console.log(out.join('\n'));
