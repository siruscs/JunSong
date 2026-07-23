package com.junsong.system.service.signal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 经营任务信号调度器。
 * 注入所有 OperatingTaskSignalGenerator 实现，统一调度执行。
 * 每个生成器独立 try-catch，一个失败不影响其他生成器。
 * 调度由外部触发（Phase 2 不接入 @Scheduled 定时任务）。
 *
 * @author junsong
 */
@Component
public class OperatingTaskSignalScheduler
{
    private static final Logger log = LoggerFactory.getLogger(OperatingTaskSignalScheduler.class);

    @Autowired(required = false)
    private List<OperatingTaskSignalGenerator> generators;

    /**
     * 运行所有信号生成器。
     *
     * @return 汇总结果：generatorCode → 新创建任务数
     */
    public Map<String, Integer> runAll()
    {
        Map<String, Integer> results = new HashMap<>();
        if (generators == null || generators.isEmpty())
        {
            log.info("无信号生成器注册，跳过调度");
            return results;
        }

        log.info("经营任务信号调度开始，共 {} 个生成器", generators.size());
        for (OperatingTaskSignalGenerator generator : generators)
        {
            String code = generator.generatorCode();
            try
            {
                int created = generator.generate();
                results.put(code, created);
                log.info("信号生成器 [{}] 执行完成，新创建任务数: {}", code, created);
            }
            catch (Exception e)
            {
                // 单个生成器失败不影响其他生成器
                results.put(code, 0);
                log.warn("信号生成器 [{}] 执行异常: {}", code, e.getMessage(), e);
            }
        }
        log.info("经营任务信号调度结束，结果: {}", results);
        return results;
    }

    /**
     * 只运行指定生成器。
     *
     * @param code 生成器标识
     * @return 新创建任务数（生成器不存在或异常返回 0）
     */
    public int runGenerator(String code)
    {
        if (generators == null || generators.isEmpty() || code == null)
        {
            return 0;
        }

        for (OperatingTaskSignalGenerator generator : generators)
        {
            if (code.equals(generator.generatorCode()))
            {
                try
                {
                    int created = generator.generate();
                    log.info("信号生成器 [{}] 单独执行完成，新创建任务数: {}", code, created);
                    return created;
                }
                catch (Exception e)
                {
                    log.warn("信号生成器 [{}] 单独执行异常: {}", code, e.getMessage(), e);
                    return 0;
                }
            }
        }
        log.warn("未找到信号生成器: {}", code);
        return 0;
    }
}
