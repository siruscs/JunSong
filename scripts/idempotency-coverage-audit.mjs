#!/usr/bin/env node
/**
 * 幂等接口覆盖清单生成脚本。
 *
 * 扫描所有 Controller 中的 @Idempotent 注解，输出接口级清单：
 * - 模块
 * - Controller 类
 * - 方法名
 * - HTTP 路径（@RequestMapping + @PostMapping/@PutMapping 等组合）
 * - HTTP 方法（POST/PUT/DELETE/PATCH）
 * - 幂等场景（@Idempotent(scene=...)）
 * - 注解属性（required / highRisk / retryPolicy / ttlSeconds）
 *
 * 用法：node scripts/idempotency-coverage-audit.mjs
 */

import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative, sep } from 'node:path';

const ROOT = process.argv[2] || process.cwd();
const MODULES_DIR = join(ROOT, 'junsong-modules');

// Controller 文件所在目录
const CONTROLLER_DIRS = [
    join(ROOT, 'junsong-modules/junsong-finance/src/main/java/com/junsong/finance/controller'),
    join(ROOT, 'junsong-modules/junsong-member/src/main/java/com/junsong/member/controller'),
    join(ROOT, 'junsong-modules/junsong-system/src/main/java/com/junsong/system/controller'),
    join(ROOT, 'junsong-modules/junsong-workflow/src/main/java/com/junsong/workflow/controller'),
    join(ROOT, 'junsong-modules/junsong-open/src/main/java/com/junsong/open/controller'),
    join(ROOT, 'junsong-modules/junsong-file/src/main/java/com/junsong/file/controller'),
    join(ROOT, 'junsong-modules/junsong-job/src/main/java/com/junsong/job/controller'),
];

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

function exists(p) {
    try { statSync(p); return true; } catch { return false; }
}

/**
 * 提取 Controller 类级别的 @RequestMapping 前缀
 */
function extractClassMapping(content) {
    const match = content.match(/@RequestMapping\s*\(\s*(?:value\s*=\s*)?["']([^"']+)["']/);
    return match ? match[1] : '';
}

/**
 * 提取 @Idempotent 注解及其所在方法的信息
 */
function extractIdempotentMethods(content, filePath) {
    const results = [];
    const className = extractClassName(content);
    const classMapping = extractClassMapping(content);

    // 按行扫描
    const lines = content.split('\n');
    let i = 0;
    while (i < lines.length) {
        const line = lines[i];

        // 匹配 @Idempotent 注解（单行或多行）
        if (line.match(/@Idempotent\s*\(/)) {
            // 收集注解完整内容（可能跨多行）
            let annotationText = '';
            let depth = 0;
            let j = i;
            while (j < lines.length) {
                annotationText += lines[j] + '\n';
                depth += (lines[j].match(/\(/g) || []).length;
                depth -= (lines[j].match(/\)/g) || []).length;
                j++;
                if (depth <= 0) break;
            }

            // 收集注解后的方法签名（跳过中间的其他注解如 @PostMapping）
            let methodStart = j;
            let methodEnd = methodStart;
            let mappingAnnotation = null;
            let methodSignature = null;

            // 向后扫描，找到方法签名（可能在同一行或后面几行）
            while (methodEnd < lines.length && methodEnd < j + 20) {
                const methodLine = lines[methodEnd];

                // 寻找 HTTP 映射注解（支持无参数形式：@PostMapping 无 value）
                const postMatch = methodLine.match(/@PostMapping\s*\(\s*(?:value\s*=\s*)?["']([^"']+)["']/);
                const postMatchNoArg = methodLine.match(/@PostMapping\s*(?:\(\s*\))?\s*$/);
                const putMatch = methodLine.match(/@PutMapping\s*\(\s*(?:value\s*=\s*)?["']([^"']+)["']/);
                const putMatchNoArg = methodLine.match(/@PutMapping\s*(?:\(\s*\))?\s*$/);
                const deleteMatch = methodLine.match(/@DeleteMapping\s*\(\s*(?:value\s*=\s*)?["']([^"']+)["']/);
                const deleteMatchNoArg = methodLine.match(/@DeleteMapping\s*(?:\(\s*\))?\s*$/);
                const patchMatch = methodLine.match(/@PatchMapping\s*\(\s*(?:value\s*=\s*)?["']([^"']+)["']/);
                const patchMatchNoArg = methodLine.match(/@PatchMapping\s*(?:\(\s*\))?\s*$/);
                const requestMatch = methodLine.match(/@RequestMapping\s*\(\s*(?:value\s*=\s*)?["']([^"']+)["']/);

                if (postMatch) { mappingAnnotation = { method: 'POST', path: postMatch[1] }; }
                else if (postMatchNoArg) { mappingAnnotation = { method: 'POST', path: '' }; }
                else if (putMatch) { mappingAnnotation = { method: 'PUT', path: putMatch[1] }; }
                else if (putMatchNoArg) { mappingAnnotation = { method: 'PUT', path: '' }; }
                else if (deleteMatch) { mappingAnnotation = { method: 'DELETE', path: deleteMatch[1] }; }
                else if (deleteMatchNoArg) { mappingAnnotation = { method: 'DELETE', path: '' }; }
                else if (patchMatch) { mappingAnnotation = { method: 'PATCH', path: patchMatch[1] }; }
                else if (patchMatchNoArg) { mappingAnnotation = { method: 'PATCH', path: '' }; }
                else if (requestMatch) {
                    // @RequestMapping 需要看 method 参数
                    const methodParam = annotationText.match(/method\s*=\s*RequestMethod\.(\w+)/);
                    mappingAnnotation = {
                        method: methodParam ? methodParam[1].toUpperCase() : 'ANY',
                        path: requestMatch[1]
                    };
                }

                // 寻找方法签名（public ReturnType methodName(...)）
                const methodMatch = methodLine.match(/public\s+\w+(?:<[^>]+>)?\s+(\w+)\s*\(/);
                if (methodMatch) {
                    methodSignature = methodMatch[1];
                    break;
                }

                // 也可能是 public AjaxResult foo(@RequestBody X x) {
                const methodMatch2 = methodLine.match(/public\s+\w+\s+(\w+)\s*\([^)]*\)/);
                if (methodMatch2) {
                    methodSignature = methodMatch2[1];
                    break;
                }

                methodEnd++;
            }

            // 解析 @Idempotent 注解属性
            const scene = extractAttr(annotationText, 'scene');
            const required = extractBool(annotationText, 'required', true);
            const highRisk = extractBool(annotationText, 'highRisk', false);
            const retryPolicy = extractAttr(annotationText, 'retryPolicy') || 'REQUIRE_NEW_KEY';
            const ttlSeconds = extractNumber(annotationText, 'ttlSeconds', 86400);

            // 拼接完整路径
            const fullPath = mappingAnnotation
                ? normalizePath(classMapping, mappingAnnotation.path)
                : '(未知)';

            results.push({
                file: relative(ROOT, filePath),
                className,
                methodName: methodSignature || '(未识别)',
                httpMethod: mappingAnnotation?.method || '(未识别)',
                httpPath: fullPath,
                scene,
                required,
                highRisk,
                retryPolicy,
                ttlSeconds,
                annotationLine: i + 1,
            });

            i = j;
            continue;
        }
        i++;
    }
    return results;
}

function extractClassName(content) {
    const match = content.match(/class\s+(\w+)/);
    return match ? match[1] : '(未知)';
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

function normalizePath(classMapping, methodPath) {
    if (!classMapping) return methodPath || '';
    if (!methodPath) return classMapping;
    return classMapping.replace(/\/$/, '') + '/' + methodPath.replace(/^\//, '');
}

// ============================================================
// 主流程
// ============================================================

const allEndpoints = [];
const moduleStats = {};

for (const dir of CONTROLLER_DIRS) {
    const files = listJavaFiles(dir);
    const moduleName = relative(join(ROOT, 'junsong-modules'), dir).split(sep)[0];

    for (const file of files) {
        const content = readFileSync(file, 'utf8');
        if (!content.includes('@Idempotent')) continue;
        const endpoints = extractIdempotentMethods(content, file);
        for (const ep of endpoints) {
            ep.module = moduleName;
            allEndpoints.push(ep);
            moduleStats[moduleName] = (moduleStats[moduleName] || 0) + 1;
        }
    }
}

// 输出 Markdown 表格
console.log('# 幂等接口覆盖清单（接口级）');
console.log('');
console.log(`> 生成时间：${new Date().toISOString()}`);
console.log(`> 扫描根目录：${ROOT}`);
console.log(`> 总端点数：${allEndpoints.length}`);
console.log('');
console.log('## 按模块统计');
console.log('');
console.log('| 模块 | 端点数 |');
console.log('|------|--------|');
for (const [m, count] of Object.entries(moduleStats).sort()) {
    console.log(`| ${m} | ${count} |`);
}
console.log(`| **合计** | **${allEndpoints.length}** |`);
console.log('');

// 按模块分组输出详细清单
const byModule = {};
for (const ep of allEndpoints) {
    if (!byModule[ep.module]) byModule[ep.module] = [];
    byModule[ep.module].push(ep);
}

for (const moduleName of Object.keys(byModule).sort()) {
    console.log(`## ${moduleName}`);
    console.log('');
    console.log('| Controller | 方法 | HTTP 方法 | HTTP 路径 | 幂等场景 | required | highRisk | retryPolicy | ttlSeconds |');
    console.log('|------------|------|-----------|-----------|----------|----------|----------|-------------|------------|');

    const endpoints = byModule[moduleName].sort((a, b) =>
        a.className.localeCompare(b.className) || a.methodName.localeCompare(b.methodName));

    for (const ep of endpoints) {
        console.log(`| ${ep.className} | ${ep.methodName} | ${ep.httpMethod} | \`${ep.httpPath}\` | ${ep.scene} | ${ep.required} | ${ep.highRisk} | ${ep.retryPolicy} | ${ep.ttlSeconds} |`);
    }
    console.log('');
}

// 输出 JSON 详情供后续分析
console.log('<!-- 详细数据（JSON）：');
console.log(JSON.stringify(allEndpoints, null, 2));
console.log('-->');
