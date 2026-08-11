SET NAMES utf8mb4;

-- 阶段 0.2：保存配置快照内容摘要，用于发布确认、差异检测和审计追踪。
-- 仅新增可空字段，历史快照保持可读；历史摘要由后续校验任务按需回填。
ALTER TABLE lc_biz_config_snapshot
    ADD COLUMN IF NOT EXISTS content_hash CHAR(64) NULL COMMENT '配置 JSON 的 SHA-256 摘要' AFTER config_json;

SELECT biz_code, version_no, content_hash,
       CHAR_LENGTH(config_json) AS config_json_length
FROM lc_biz_config_snapshot
WHERE del_flag = '0'
ORDER BY biz_code, version_no;
