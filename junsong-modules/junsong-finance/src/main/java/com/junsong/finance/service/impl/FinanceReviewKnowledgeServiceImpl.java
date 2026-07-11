package com.junsong.finance.service.impl;

import com.junsong.common.core.constant.SecurityConstants;
import com.junsong.common.core.domain.R;
import com.junsong.common.core.exception.ServiceException;
import com.junsong.common.security.utils.SecurityUtils;
import com.junsong.finance.domain.FinanceReviewKnowledge;
import com.junsong.finance.domain.FinanceReviewTask;
import com.junsong.finance.mapper.FinanceReviewKnowledgeMapper;
import com.junsong.finance.mapper.FinanceReviewTaskMapper;
import com.junsong.finance.service.IFinanceReviewKnowledgeService;
import com.junsong.system.api.RemoteUserService;
import com.junsong.system.api.domain.SysDept;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 复盘知识库Service实现
 *
 * @author junsong
 */
@Service
public class FinanceReviewKnowledgeServiceImpl implements IFinanceReviewKnowledgeService {

    private static final Logger log = LoggerFactory.getLogger(FinanceReviewKnowledgeServiceImpl.class);

    private static final String STATUS_DONE = "DONE";

    @Autowired
    private FinanceReviewKnowledgeMapper knowledgeMapper;

    @Autowired
    private FinanceReviewTaskMapper reviewTaskMapper;

    @Autowired
    private RemoteUserService remoteUserService;

    @Override
    public List<FinanceReviewKnowledge> listKnowledge(Map<String, Object> params) {
        List<Long> allowed = loadAllowedDeptIds();
        if (!allowed.isEmpty()) {
            params.put("allowedDeptIds", allowed);
        }
        return knowledgeMapper.selectKnowledgeList(params);
    }

    @Override
    public FinanceReviewKnowledge getByKnowledgeId(Long knowledgeId) {
        return knowledgeMapper.selectByKnowledgeId(knowledgeId);
    }

    @Override
    public int addKnowledge(FinanceReviewKnowledge knowledge) {
        validateKnowledge(knowledge);
        // R11-FIX-D: 明确全局知识/门店知识 scope
        // - deptId == null → 全局知识，仅管理员可创建（当前只授权 admin）
        // - deptId != null → 门店知识，必须校验门店授权
        if (knowledge.getDeptId() != null) {
            verifyDeptAccess(knowledge.getDeptId());
        }
        knowledge.setCreateTime(new Date());
        return knowledgeMapper.insertKnowledge(knowledge);
    }

    @Override
    public int updateKnowledge(FinanceReviewKnowledge knowledge) {
        if (knowledge.getKnowledgeId() == null) {
            throw new ServiceException("知识ID不能为空");
        }
        FinanceReviewKnowledge existing = knowledgeMapper.selectByKnowledgeId(knowledge.getKnowledgeId());
        if (existing == null) {
            throw new ServiceException("知识条目不存在");
        }
        verifyDeptAccess(existing.getDeptId());
        validateKnowledge(knowledge);
        knowledge.setUpdateTime(new Date());
        return knowledgeMapper.updateKnowledge(knowledge);
    }

    @Override
    public FinanceReviewKnowledge createFromTask(Long taskId, Map<String, String> body) {
        // 1. 加载并校验任务
        FinanceReviewTask task = reviewTaskMapper.selectByTaskId(taskId);
        if (task == null) {
            throw new ServiceException("复盘任务不存在");
        }

        // 2. 校验门店授权
        verifyDeptAccess(task.getDeptId());

        // 3. 任务必须已完成
        if (!STATUS_DONE.equals(task.getStatus())) {
            throw new ServiceException("仅已完成的任务可沉淀知识");
        }

        // 4. 检查重复
        FinanceReviewKnowledge existing = knowledgeMapper.selectReusableByTaskId(taskId);
        if (existing != null) {
            throw new ServiceException("该任务已沉淀过知识，不可重复创建");
        }

        // 5. 构建知识条目
        String actionTaken = body.get("actionTaken");
        if (actionTaken == null || actionTaken.trim().isEmpty()) {
            // 默认使用任务的处理备注
            actionTaken = task.getHandlerNote();
        }
        if (actionTaken == null || actionTaken.trim().isEmpty()) {
            throw new ServiceException("采取动作不能为空");
        }

        String problemType = body.get("problemType");
        if (problemType == null || problemType.trim().isEmpty()) {
            problemType = task.getTaskType();
        }

        FinanceReviewKnowledge knowledge = new FinanceReviewKnowledge();
        knowledge.setTaskId(taskId);
        knowledge.setDeptId(task.getDeptId());
        knowledge.setProblemType(problemType);
        knowledge.setTitle(task.getTitle());
        knowledge.setProblemSummary(task.getReason() != null ? task.getReason() : "");
        knowledge.setRootCause(body.get("rootCause"));
        knowledge.setActionTaken(actionTaken);
        knowledge.setResultSummary(body.get("resultSummary"));
        knowledge.setReusable("1");
        knowledge.setSourceHandlerName(task.getHandlerName());
        knowledge.setCreateBy(SecurityUtils.getUsername());
        knowledge.setCreateTime(new Date());

        knowledgeMapper.insertKnowledge(knowledge);
        log.info("Created review knowledge from task {}: problemType={}", taskId, problemType);
        return knowledge;
    }

    @Override
    public List<FinanceReviewKnowledge> getRecentReusable(List<String> problemTypes, int limit) {
        return knowledgeMapper.selectRecentReusable(problemTypes, limit);
    }

    @Override
    public List<FinanceReviewKnowledge> recommendForTask(Long taskId) {
        FinanceReviewTask task = reviewTaskMapper.selectByTaskId(taskId);
        if (task == null) {
            throw new ServiceException("任务不存在");
        }
        // Verify dept access
        verifyDeptAccess(task.getDeptId());

        // Extract keywords from task title and problem type
        List<String> keywords = new ArrayList<>();
        if (task.getTitle() != null && !task.getTitle().isEmpty()) {
            keywords.add(task.getTitle());
        }
        if (task.getTaskType() != null && !task.getTaskType().isEmpty()) {
            keywords.add(task.getTaskType());
        }

        List<Long> allowedDeptIds = loadAllowedDeptIds();
        return knowledgeMapper.selectRecommendations(
                task.getTaskType(),
                task.getDeptId(),
                keywords,
                allowedDeptIds.isEmpty() ? null : allowedDeptIds,
                5
        );
    }

    // ─── Internal helpers ────────────────────────────────────────────────────

    private void validateKnowledge(FinanceReviewKnowledge knowledge) {
        if (knowledge.getProblemType() == null || knowledge.getProblemType().trim().isEmpty()) {
            throw new ServiceException("问题类型不能为空");
        }
        if (knowledge.getTitle() == null || knowledge.getTitle().trim().isEmpty()) {
            throw new ServiceException("知识标题不能为空");
        }
        if (knowledge.getProblemSummary() == null || knowledge.getProblemSummary().trim().isEmpty()) {
            throw new ServiceException("问题摘要不能为空");
        }
        if (knowledge.getActionTaken() == null || knowledge.getActionTaken().trim().isEmpty()) {
            throw new ServiceException("采取动作不能为空");
        }
    }

    /**
     * Verify current user has access to the given department.
     */
    private void verifyDeptAccess(Long deptId) {
        if (SecurityUtils.isAdmin(SecurityUtils.getUserId())) {
            return;
        }
        List<Long> allowed = loadAllowedDeptIds();
        if (!allowed.contains(deptId)) {
            throw new ServiceException("无权操作此门店的复盘知识");
        }
    }

    private List<Long> loadAllowedDeptIds() {
        if (SecurityUtils.isAdmin(SecurityUtils.getUserId())) {
            return Collections.emptyList();
        }
        try {
            if (remoteUserService != null) {
                R<List<SysDept>> result = remoteUserService.getUserDeptList(
                        SecurityUtils.getUsername(), SecurityConstants.INNER);
                if (result != null && result.getCode() == 200 && result.getData() != null && !result.getData().isEmpty()) {
                    return result.getData().stream().map(SysDept::getDeptId).collect(Collectors.toList());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to load user dept list from remote service", e);
        }
        Long currentDeptId = SecurityUtils.getDeptId();
        if (currentDeptId != null) {
            return Collections.singletonList(currentDeptId);
        }
        return Collections.singletonList(-1L);
    }
}
