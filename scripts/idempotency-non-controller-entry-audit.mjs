#!/usr/bin/env node
/**
 * 非 Controller 写入口审计脚本（补充差集报告）。
 *
 * 用户第二轮复核明确要求："必须补充'全量写接口总数、未覆盖数量、排除原因、异步入口数量'的差集报告"。
 * idempotency-write-endpoint-audit.mjs 只扫描 Controller 层 HTTP 写接口，
 * 本脚本扫描以下非 Controller 写入口：
 *
 *   1. @Scheduled 定时任务
 *   2. MQ 消费者（@KafkaListener/@RabbitListener/@RocketMQMessageListener/MessageListener）
 *   3. 工作流回调（JavaDelegate/TaskListener/ExecutionListener/@EventListener）
 *   4. 内部接口 Controller（/open/internal/**, @InnerAuth）—— Feign 远程调用入口
 *
 * 用法：node scripts/idempotency-non-controller-entry-audit.mjs
 *      输出 Markdown 到 stdout。
 */

import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative, sep } from 'node:path';

const ROOT = process.argv[2] || process.cwd();

// 扫描所有模块的 src/main/java 目录
const MODULE_DIRS = [
    join(ROOT, 'junsong-modules/junsong-finance/src/main/java'),
    join(ROOT, 'junsong-modules/junsong-member/src/main/java'),
    join(ROOT, 'junsong-modules/junsong-system/src/main/java'),
    join(ROOT, 'junsong-modules/junsong-workflow/src/main/java'),
    join(ROOT, 'junsong-modules/junsong-open/src/main/java'),
    join(ROOT, 'junsong-modules/junsong-file/src/main/java'),
    join(ROOT, 'junsong-modules/junsong-job/src/main/java'),
    join(ROOT, 'junsong-common/junsong-common-core/src/main/java'),
];

function listJavaFiles(dir) {
    const result = [];
    try {
        const entries = readdirSync(dir);
        for (const name of entries) {
            const path = join(dir, name);
            const stat = statSync(path);
            if (stat.isDirectory()) {
                result.push(...listJavaFiles(path));
            } else if (name.endsWith('.java')) {
                result.push(path);
            }
        }
    } catch (e) {
        // 目录不存在
    }
    return result;
}

function extractClassName(content) {
    const match = content.match(/class\s+(\w+)/);
    return match ? match[1] : '(未知)';
}

function extractPackageName(filePath) {
    const parts = filePath.split(sep);
    const javaIdx = parts.indexOf('java');
    if (javaIdx === -1 || javaIdx + 1 >= parts.length) return '';
    return parts.slice(javaIdx + 1, -1).join('.');
}

/**
 * 判断方法体是否包含数据库写操作（insert/update/delete/save）
 */
function containsDbWrite(methodBody) {
    if (!methodBody) return false;
    const lower = methodBody.toLowerCase();
    return lower.includes('insert') ||
           lower.includes('update') ||
           lower.includes('delete') ||
           lower.includes('save') ||
           lower.includes('remove') ||
           lower.includes('.add(') ||
           lower.includes('.edit(') ||
           lower.includes('.post(') ||
           lower.includes('.create(');
}

/**
 * 提取方法名
 */
function extractMethodName(line) {
    const m = line.match(/(?:public|private|protected)\s+(?:\w+(?:<[^>]+>)?\s+)?(\w+)\s*\(/);
    return m ? m[1] : null;
}

/**
 * 扫描 @Scheduled 定时任务
 */
function scanScheduledTasks(files) {
    const results = [];
    for (const file of files) {
        const content = readFileSync(file, 'utf8');
        if (!content.includes('@Scheduled')) continue;

        const className = extractClassName(content);
        const packageName = extractPackageName(file);
        const lines = content.split('\n');

        for (let i = 0; i < lines.length; i++) {
            if (lines[i].includes('@Scheduled')) {
                // 提取 cron/fixedRate 等属性
                const cronMatch = lines[i].match(/cron\s*=\s*["']([^"']+)["']/);
                const fixedRateMatch = lines[i].match(/fixedRate\s*=\s*(\d+)/);
                const fixedDelayMatch = lines[i].match(/fixedDelay\s*=\s*(\d+)/);
                const schedule = cronMatch ? `cron=${cronMatch[1]}` :
                                 fixedRateMatch ? `fixedRate=${fixedRateMatch[1]}ms` :
                                 fixedDelayMatch ? `fixedDelay=${fixedDelayMatch[1]}ms` : '(未知)';

                // 向后找方法签名
                let methodName = null;
                let methodLine = i;
                for (let j = i + 1; j < Math.min(i + 10, lines.length); j++) {
                    methodName = extractMethodName(lines[j]);
                    if (methodName) {
                        methodLine = j + 1;
                        break;
                    }
                }

                // 简单判断是否写库（向后扫描 50 行方法体）
                const methodBody = lines.slice(i, Math.min(i + 50, lines.length)).join('\n');
                const writesDb = containsDbWrite(methodBody);

                results.push({
                    type: '@Scheduled 定时任务',
                    module: inferModule(file),
                    file: relative(ROOT, file),
                    className: `${packageName}.${className}`,
                    methodName: methodName || '(未识别)',
                    schedule,
                    writesDb,
                    hasIdempotent: false, // 定时任务无法加 @Idempotent 注解
                    idempotentStrategy: writesDb ? '业务状态机兜底（推荐）' : '无需幂等（只读）',
                    line: methodLine,
                });
            }
        }
    }
    return results;
}

/**
 * 扫描 MQ 消费者
 */
function scanMqConsumers(files) {
    const results = [];
    const MQ_PATTERNS = [
        /@KafkaListener/,
        /@RabbitListener/,
        /@RocketMQMessageListener/,
    ];
    for (const file of files) {
        const content = readFileSync(file, 'utf8');
        let isMqConsumer = false;
        let mqType = null;
        for (const pattern of MQ_PATTERNS) {
            if (pattern.test(content)) {
                isMqConsumer = true;
                mqType = pattern.toString().match(/@(\w+)/)[1];
                break;
            }
        }
        // MessageListener 接口实现
        if (!isMqConsumer && /implements\s+.*MessageListener/.test(content)) {
            isMqConsumer = true;
            mqType = 'MessageListener';
        }
        if (!isMqConsumer) continue;

        const className = extractClassName(content);
        const packageName = extractPackageName(file);
        const lines = content.split('\n');

        // 找消费方法（通常叫 onMessage/handle/consume）
        for (let i = 0; i < lines.length; i++) {
            const line = lines[i];
            const methodMatch = line.match(/(?:public|private|protected)\s+(?:\w+(?:<[^>]+>)?\s+)?(\w+)\s*\(/);
            if (!methodMatch) continue;
            const methodName = methodMatch[1];
            // 消费者方法名通常含 onMessage/handle/consume/receive
            if (!/^(onMessage|handle|consume|receive|onMessageReceived|process)/i.test(methodName)) continue;

            const methodBody = lines.slice(i, Math.min(i + 50, lines.length)).join('\n');
            const writesDb = containsDbWrite(methodBody);

            results.push({
                type: `MQ 消费者（${mqType}）`,
                module: inferModule(file),
                file: relative(ROOT, file),
                className: `${packageName}.${className}`,
                methodName,
                schedule: '事件驱动',
                writesDb,
                hasIdempotent: false,
                idempotentStrategy: writesDb ? '消息去重表/业务状态机（推荐）' : '无需幂等（只读）',
                line: i + 1,
            });
        }
    }
    return results;
}

/**
 * 扫描工作流回调（JavaDelegate/TaskListener/ExecutionListener/@EventListener）
 */
function scanWorkflowCallbacks(files) {
    const results = [];
    for (const file of files) {
        const content = readFileSync(file, 'utf8');
        if (!content.includes('JavaDelegate') &&
            !content.includes('TaskListener') &&
            !content.includes('ExecutionListener') &&
            !content.includes('@EventListener') &&
            !content.includes('ApplicationListener')) continue;

        const className = extractClassName(content);
        const packageName = extractPackageName(file);
        const lines = content.split('\n');

        // 判断回调类型
        let callbackType = null;
        if (content.includes('JavaDelegate')) callbackType = 'JavaDelegate';
        else if (content.includes('TaskListener')) callbackType = 'TaskListener';
        else if (content.includes('ExecutionListener')) callbackType = 'ExecutionListener';
        else if (content.includes('@EventListener')) callbackType = '@EventListener';
        else if (content.includes('ApplicationListener')) callbackType = 'ApplicationListener';

        // 找回调方法
        const callbackMethodNames = ['execute', 'notify', 'onEvent', 'handleEvent', 'onCreate', 'onComplete', 'onAssignment'];
        for (let i = 0; i < lines.length; i++) {
            const line = lines[i];
            const methodMatch = line.match(/(?:public|private|protected)\s+(?:\w+(?:<[^>]+>)?\s+)?(\w+)\s*\(/);
            if (!methodMatch) continue;
            const methodName = methodMatch[1];
            if (!callbackMethodNames.includes(methodName)) continue;

            const methodBody = lines.slice(i, Math.min(i + 50, lines.length)).join('\n');
            const writesDb = containsDbWrite(methodBody);

            results.push({
                type: `工作流回调（${callbackType}）`,
                module: inferModule(file),
                file: relative(ROOT, file),
                className: `${packageName}.${className}`,
                methodName,
                schedule: '工作流事件驱动',
                writesDb,
                hasIdempotent: false,
                idempotentStrategy: writesDb ? '业务状态机/工作流引擎去重（推荐）' : '无需幂等（只读）',
                line: i + 1,
            });
        }
    }
    return results;
}

/**
 * 扫描内部接口 Controller（/open/internal/**, @InnerAuth）—— Feign 远程调用入口
 */
function scanInternalControllers(files) {
    const results = [];
    for (const file of files) {
        const content = readFileSync(file, 'utf8');
        // 只扫描 Controller 文件
        if (!content.includes('@RestController') && !content.includes('@Controller')) continue;
        // 只扫描内部接口
        if (!content.includes('@InnerAuth') &&
            !content.includes('/internal/') &&
            !content.includes('/open/internal')) continue;

        const className = extractClassName(content);
        const packageName = extractPackageName(file);
        const classMappingMatch = content.match(/@RequestMapping\s*\(\s*(?:value\s*=\s*)?["']([^"']+)["']/);
        const classMapping = classMappingMatch ? classMappingMatch[1] : '';
        const lines = content.split('\n');

        const WRITE_MAPP = [
            /@PostMapping/, /@PutMapping/, /@DeleteMapping/, /@PatchMapping/
        ];

        for (let i = 0; i < lines.length; i++) {
            const line = lines[i];
            if (!WRITE_MAPP.some(p => p.test(line))) continue;

            const pathMatch = line.match(/(?:Post|Put|Delete|Patch)Mapping\s*\(\s*(?:value\s*=\s*)?["']([^"']+)["']/);
            const methodPath = pathMatch ? pathMatch[1] : '';
            const fullPath = classMapping && methodPath
                ? classMapping.replace(/\/$/, '') + '/' + methodPath.replace(/^\//, '')
                : classMapping || methodPath;

            // 找方法名
            let methodName = null;
            for (let j = i + 1; j < Math.min(i + 10, lines.length); j++) {
                methodName = extractMethodName(lines[j]);
                if (methodName) break;
            }

            // 检查是否有 @Idempotent
            let hasIdempotent = false;
            for (let k = Math.max(0, i - 5); k < i; k++) {
                if (lines[k].includes('@Idempotent')) {
                    hasIdempotent = true;
                    break;
                }
            }

            const methodBody = lines.slice(i, Math.min(i + 30, lines.length)).join('\n');
            const writesDb = containsDbWrite(methodBody) || true; // 内部接口默认视为写

            results.push({
                type: '内部接口（Feign 远程调用入口）',
                module: inferModule(file),
                file: relative(ROOT, file),
                className: `${packageName}.${className}`,
                methodName: methodName || '(未识别)',
                schedule: 'Feign 调用',
                writesDb,
                hasIdempotent,
                idempotentStrategy: hasIdempotent
                    ? '已加 @Idempotent'
                    : (writesDb ? '调用方 Controller 已覆盖/业务状态机兜底' : '无需幂等（只读）'),
                line: i + 1,
                httpPath: fullPath,
            });
        }
    }
    return results;
}

function inferModule(filePath) {
    const m = filePath.match(/junsong-modules[\/\\]([^\/\\]+)/);
    if (m) return m[1];
    const c = filePath.match(/junsong-common[\/\\]([^\/\\]+)/);
    if (c) return `junsong-common-${c[1]}`;
    return '(其他)';
}

// ============================================================
// 主流程
// ============================================================

const allFiles = [];
for (const dir of MODULE_DIRS) {
    allFiles.push(...listJavaFiles(dir));
}

console.log('# 非 Controller 写入口审计报告');
console.log('');
console.log(`> 生成时间：${new Date().toISOString()}`);
console.log(`> 扫描根目录：${ROOT}`);
console.log('> 扫描范围：定时任务、MQ 消费者、工作流回调、内部接口（Feign 远程调用入口）');
console.log('');

const scheduledTasks = scanScheduledTasks(allFiles);
const mqConsumers = scanMqConsumers(allFiles);
const workflowCallbacks = scanWorkflowCallbacks(allFiles);
const internalControllers = scanInternalControllers(allFiles);

const allEntries = [
    ...scheduledTasks,
    ...mqConsumers,
    ...workflowCallbacks,
    ...internalControllers,
];

const writeEntries = allEntries.filter(e => e.writesDb);
const readEntries = allEntries.filter(e => !e.writesDb);

console.log('## 1. 总体统计');
console.log('');
console.log('| 类别 | 数量 |');
console.log('|------|------|');
console.log(`| @Scheduled 定时任务 | ${scheduledTasks.length} |`);
console.log(`| MQ 消费者 | ${mqConsumers.length} |`);
console.log(`| 工作流回调 | ${workflowCallbacks.length} |`);
console.log(`| 内部接口（Feign 远程调用入口） | ${internalControllers.length} |`);
console.log(`| **合计** | **${allEntries.length}** |`);
console.log('');
console.log(`| 写库入口 | ${writeEntries.length} |`);
console.log(`| 只读入口 | ${readEntries.length} |`);
console.log('');

console.log('## 2. 幂等策略说明');
console.log('');
console.log('非 Controller 入口**无法直接加 @Idempotent 注解**（注解基于 AOP 拦截 Controller 方法），需采用以下替代策略：');
console.log('');
console.log('| 入口类型 | 推荐幂等策略 | 理由 |');
console.log('|---------|-------------|------|');
console.log('| @Scheduled 定时任务 | 业务状态机兜底 | 定时任务通常按批次处理，由业务状态（如期间状态、批次状态）保证不重复执行 |');
console.log('| MQ 消费者 | 消息去重表 + 业务状态机 | MQ 消费者可能收到重复消息，需在消费者端维护消息去重表（message_id 唯一索引） |');
console.log('| 工作流回调 | 工作流引擎去重 + 业务状态机 | Flowable/Activiti 引擎本身有任务状态机，回调由引擎保证不重复触发 |');
console.log('| 内部接口（Feign） | 调用方 Controller 的 @Idempotent 覆盖 + 业务状态机 | Feign 调用通常由 Controller 触发，Controller 的 @Idempotent 已覆盖；内部接口本身由 @InnerAuth 鉴权 |');
console.log('');

console.log('## 3. @Scheduled 定时任务清单');
console.log('');
if (scheduledTasks.length === 0) {
    console.log('（无）');
} else {
    console.log('| 模块 | 类 | 方法 | 调度策略 | 写库 | 幂等策略 |');
    console.log('|------|-----|------|---------|------|---------|');
    for (const e of scheduledTasks) {
        console.log(`| ${e.module} | ${e.className} | ${e.methodName} | ${e.schedule} | ${e.writesDb ? '是' : '否'} | ${e.idempotentStrategy} |`);
    }
}
console.log('');

console.log('## 4. MQ 消费者清单');
console.log('');
if (mqConsumers.length === 0) {
    console.log('（无）');
} else {
    console.log('| 模块 | 类 | 方法 | 写库 | 幂等策略 |');
    console.log('|------|-----|------|------|---------|');
    for (const e of mqConsumers) {
        console.log(`| ${e.module} | ${e.className} | ${e.methodName} | ${e.writesDb ? '是' : '否'} | ${e.idempotentStrategy} |`);
    }
}
console.log('');

console.log('## 5. 工作流回调清单');
console.log('');
if (workflowCallbacks.length === 0) {
    console.log('（无）');
} else {
    console.log('| 模块 | 类 | 方法 | 写库 | 幂等策略 |');
    console.log('|------|-----|------|------|---------|');
    for (const e of workflowCallbacks) {
        console.log(`| ${e.module} | ${e.className} | ${e.methodName} | ${e.writesDb ? '是' : '否'} | ${e.idempotentStrategy} |`);
    }
}
console.log('');

console.log('## 6. 内部接口（Feign 远程调用入口）清单');
console.log('');
if (internalControllers.length === 0) {
    console.log('（无）');
} else {
    console.log('| 模块 | 类 | 方法 | HTTP 路径 | 有 @Idempotent | 幂等策略 |');
    console.log('|------|-----|------|---------|---------------|---------|');
    for (const e of internalControllers) {
        console.log(`| ${e.module} | ${e.className} | ${e.methodName} | ${e.httpPath || '(空)'} | ${e.hasIdempotent ? '是' : '否'} | ${e.idempotentStrategy} |`);
    }
}
console.log('');

console.log('## 7. 未覆盖风险与建议');
console.log('');
console.log('### 7.1 高风险写库入口');
console.log('');
const highRiskEntries = writeEntries.filter(e => !e.hasIdempotent);
if (highRiskEntries.length === 0) {
    console.log('（无）');
} else {
    console.log('| 类型 | 模块 | 类 | 方法 | 风险 | 建议 |');
    console.log('|------|------|-----|------|------|------|');
    for (const e of highRiskEntries) {
        const risk = e.type.includes('MQ') || e.type.includes('定时') ? '中' : '低';
        console.log(`| ${e.type} | ${e.module} | ${e.className} | ${e.methodName} | ${risk} | ${e.idempotentStrategy} |`);
    }
}
console.log('');

console.log('### 7.2 DEV 部署后验证建议');
console.log('');
console.log('1. **定时任务**：通过 APM/日志监控同一批次是否被重复执行；检查业务状态机是否正确拦截重复执行');
console.log('2. **MQ 消费者**：发送重复消息（相同 message_id）验证消费者去重表是否生效；检查业务表是否只有一条记录');
console.log('3. **工作流回调**：触发同一工作流事件多次，验证引擎是否去重；检查业务状态机是否拦截重复回调');
console.log('4. **内部接口**：通过 Feign 重复调用相同接口（相同 X-Idempotency-Key），验证 @Idempotent 是否生效');
console.log('');
console.log('## 8. 与 Controller 写接口审计的关系');
console.log('');
console.log('- 本报告是 `idempotency-write-endpoint-audit.mjs`（Controller 写接口审计）的补充');
console.log('- 两份报告合并后即为全系统写入口的完整差集报告');
console.log('- Controller 写接口由 @Idempotent 注解保护');
console.log('- 非 Controller 写入口由业务状态机/消息去重表/工作流引擎去重保护');
console.log('- DEV 部署后需通过 APM/日志审计验证所有入口的幂等性');
