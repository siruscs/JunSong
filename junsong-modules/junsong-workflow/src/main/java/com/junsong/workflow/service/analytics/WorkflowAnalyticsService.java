package com.junsong.workflow.service.analytics;

import java.util.*;
import java.util.stream.Collectors;

import com.junsong.common.core.domain.R;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.api.history.HistoricTaskInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class WorkflowAnalyticsService
{
    @Autowired
    private HistoryService historyService;
    @Autowired
    private RepositoryService repositoryService;

    public R<List<Map<String, Object>>> nodeDurationStats(String processDefinitionKey)
    {
        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .finished()
                .list();

        Map<String, List<Long>> grouped = new HashMap<>();
        Map<String, String> nameMap = new HashMap<>();
        for (HistoricTaskInstance t : tasks)
        {
            String key = t.getTaskDefinitionKey();
            Long duration = t.getDurationInMillis();
            if (duration != null)
            {
                grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(duration);
                nameMap.putIfAbsent(key, t.getName());
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<Long>> entry : grouped.entrySet())
        {
            List<Long> durations = entry.getValue();
            long sum = durations.stream().mapToLong(Long::longValue).sum();
            long avg = sum / durations.size();
            long max = durations.stream().mapToLong(Long::longValue).max().orElse(0);
            long min = durations.stream().mapToLong(Long::longValue).min().orElse(0);

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("activityId", entry.getKey());
            m.put("activityName", nameMap.get(entry.getKey()));
            m.put("taskCount", durations.size());
            m.put("avgDuration", avg);
            m.put("avgDurationFormatted", formatDuration(avg));
            m.put("maxDuration", max);
            m.put("maxDurationFormatted", formatDuration(max));
            m.put("minDuration", min);
            m.put("minDurationFormatted", formatDuration(min));
            result.add(m);
        }
        result.sort((a, b) -> Long.compare((Long) b.get("avgDuration"), (Long) a.get("avgDuration")));
        return R.ok(result);
    }

    public R<List<Map<String, Object>>> userEfficiencyStats()
    {
        List<HistoricTaskInstance> tasks = historyService.createHistoricTaskInstanceQuery()
                .finished()
                .list();

        Map<String, List<Long>> grouped = new HashMap<>();
        for (HistoricTaskInstance t : tasks)
        {
            String assignee = t.getAssignee();
            Long duration = t.getDurationInMillis();
            if (assignee != null && duration != null)
            {
                grouped.computeIfAbsent(assignee, k -> new ArrayList<>()).add(duration);
            }
        }

        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<String, List<Long>> entry : grouped.entrySet())
        {
            List<Long> durations = entry.getValue();
            long sum = durations.stream().mapToLong(Long::longValue).sum();
            long avg = sum / durations.size();

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("assignee", entry.getKey());
            m.put("totalTasks", durations.size());
            m.put("avgDuration", avg);
            m.put("avgDurationFormatted", formatDuration(avg));
            result.add(m);
        }
        result.sort((a, b) -> Long.compare((Long) a.get("avgDuration"), (Long) b.get("avgDuration")));
        return R.ok(result);
    }

    public R<Map<String, Object>> processDurationStats(String processDefinitionKey)
    {
        List<HistoricProcessInstance> instances = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .finished()
                .list();

        List<Long> durations = instances.stream()
                .map(HistoricProcessInstance::getDurationInMillis)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (durations.isEmpty())
        {
            Map<String, Object> empty = new LinkedHashMap<>();
            empty.put("totalInstances", 0);
            return R.ok(empty);
        }

        long sum = durations.stream().mapToLong(Long::longValue).sum();
        long avg = sum / durations.size();
        long max = durations.stream().mapToLong(Long::longValue).max().orElse(0);
        long min = durations.stream().mapToLong(Long::longValue).min().orElse(0);

        // 计算中位数
        Collections.sort(durations);
        long median = durations.get(durations.size() / 2);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalInstances", durations.size());
        result.put("avgDuration", avg);
        result.put("avgDurationFormatted", formatDuration(avg));
        result.put("maxDuration", max);
        result.put("maxDurationFormatted", formatDuration(max));
        result.put("minDuration", min);
        result.put("minDurationFormatted", formatDuration(min));
        result.put("medianDuration", median);
        result.put("medianDurationFormatted", formatDuration(median));
        return R.ok(result);
    }

    private String formatDuration(long millis)
    {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        if (days > 0) return days + "天" + (hours % 24) + "小时";
        if (hours > 0) return hours + "小时" + (minutes % 60) + "分";
        if (minutes > 0) return minutes + "分" + (seconds % 60) + "秒";
        return seconds + "秒";
    }
}
