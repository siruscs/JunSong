-- 会签/或签功能：为审批节点配置增加多人审批选项
ALTER TABLE lc_biz_node_assignee
    ADD COLUMN multi_instance_type VARCHAR(20) DEFAULT 'none' COMMENT '多实例类型: none(单人)/parallel(并行会签)/sequential(串行会签)',
    ADD COLUMN completion_condition VARCHAR(200) DEFAULT '' COMMENT '完成条件表达式，如: ${nrOfCompletedInstances/nrOfInstances >= 1}',
    ADD COLUMN collection_source VARCHAR(50) DEFAULT '' COMMENT '人员集合来源: fixed_users/role/form_field/dept_leader/initiator_leader';
