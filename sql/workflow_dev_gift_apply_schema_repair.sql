SET NAMES utf8mb4;
-- DEV gift_apply 详情页补齐：使用现有字段生成 FORM/LIST/DETAIL 页面 Schema，随后发布版本 1。
-- 可重复执行，不覆盖已有页面或已发布快照。
INSERT INTO lc_biz_page_schema
    (biz_code, page_type, schema_json, version, status, del_flag, create_by, create_time, update_by, update_time)
SELECT 'gift_apply', p.page_type,
       '{"fields":["gift_name","shuliang","paymethod","needdate","reason"]}',
       1, '1', '0', 'dev-schema-repair', NOW(), 'dev-schema-repair', NOW()
FROM (SELECT 'FORM' page_type UNION ALL SELECT 'LIST' UNION ALL SELECT 'DETAIL') p
WHERE NOT EXISTS (
    SELECT 1 FROM lc_biz_page_schema s
    WHERE s.biz_code = 'gift_apply' AND s.page_type = p.page_type AND s.del_flag = '0'
);

INSERT INTO lc_biz_config_snapshot
    (biz_code, version_no, config_json, status, publish_remark, del_flag, create_by, create_time, update_by, update_time, remark)
SELECT 'gift_apply', 1,
       JSON_OBJECT(
           'bizObject', JSON_OBJECT('bizCode', 'gift_apply', 'bizName', '礼品申请', 'configStatus', 'PUBLISHED', 'publishedVersion', 1,
               'workflowEnabled', '1', 'processKey', 'gift_apply', 'storageMode', 'GENERIC'),
           'fields', NULL,
           'pageSchemas', JSON_ARRAY(
               JSON_OBJECT('bizCode', 'gift_apply', 'pageType', 'FORM', 'schemaJson', JSON_UNQUOTE(JSON_QUOTE('{"fields":["gift_name","shuliang","paymethod","needdate","reason"]}'))),
               JSON_OBJECT('bizCode', 'gift_apply', 'pageType', 'LIST', 'schemaJson', JSON_UNQUOTE(JSON_QUOTE('{"fields":["gift_name","shuliang","paymethod","needdate","reason"]}'))),
               JSON_OBJECT('bizCode', 'gift_apply', 'pageType', 'DETAIL', 'schemaJson', JSON_UNQUOTE(JSON_QUOTE('{"fields":["gift_name","shuliang","paymethod","needdate","reason"]}')))
           )
       ),
       'PUBLISHED', 'DEV schema repair', '0', 'dev-schema-repair', NOW(), '', NULL, 'DEV gift_apply schema repair'
WHERE NOT EXISTS (
    SELECT 1 FROM lc_biz_config_snapshot s
    WHERE s.biz_code = 'gift_apply' AND s.status = 'PUBLISHED' AND s.del_flag = '0'
);

-- 修复早期脚本已写入但未正确转义的发布快照；仅命中 gift_apply 的非法 schemaJson 快照。
UPDATE lc_biz_config_snapshot
SET config_json = JSON_OBJECT(
        'bizObject', JSON_OBJECT('bizCode', 'gift_apply', 'bizName', '礼品申请', 'configStatus', 'PUBLISHED', 'publishedVersion', 1,
            'workflowEnabled', '1', 'processKey', 'gift_apply', 'storageMode', 'GENERIC'),
        'fields', NULL,
        'pageSchemas', JSON_ARRAY(
            JSON_OBJECT('bizCode', 'gift_apply', 'pageType', 'FORM', 'schemaJson', JSON_UNQUOTE(JSON_QUOTE('{"fields":["gift_name","shuliang","paymethod","needdate","reason"]}'))),
            JSON_OBJECT('bizCode', 'gift_apply', 'pageType', 'LIST', 'schemaJson', JSON_UNQUOTE(JSON_QUOTE('{"fields":["gift_name","shuliang","paymethod","needdate","reason"]}'))),
            JSON_OBJECT('bizCode', 'gift_apply', 'pageType', 'DETAIL', 'schemaJson', JSON_UNQUOTE(JSON_QUOTE('{"fields":["gift_name","shuliang","paymethod","needdate","reason"]}')))
        )
    ),
    update_time = NOW(), update_by = 'dev-schema-repair'
WHERE biz_code = 'gift_apply'
  AND status = 'PUBLISHED'
  AND del_flag = '0'
  AND (
      config_json LIKE CONCAT('%', CHAR(34), 'schemaJson', CHAR(34), ':', CHAR(34), '{', CHAR(34), 'fields', CHAR(34), '%')
      OR (JSON_TYPE(JSON_EXTRACT(config_json, '$.fields')) = 'ARRAY'
          AND JSON_LENGTH(JSON_EXTRACT(config_json, '$.fields')) = 0)
      OR JSON_EXTRACT(config_json, '$.bizObject.workflowEnabled') IS NULL
      OR JSON_EXTRACT(config_json, '$.bizObject.processKey') IS NULL
  );

UPDATE lc_biz_object
SET config_status = 'PUBLISHED', published_version = 1, update_by = 'dev-schema-repair', update_time = NOW()
WHERE biz_code = 'gift_apply' AND (config_status <> 'PUBLISHED' OR published_version <> 1);

SELECT 'RECONCILIATION' result_type,
       (SELECT COUNT(*) FROM lc_biz_page_schema WHERE biz_code = 'gift_apply' AND page_type = 'DETAIL' AND del_flag = '0') detail_schema_count,
       (SELECT COUNT(*) FROM lc_biz_config_snapshot WHERE biz_code = 'gift_apply' AND status = 'PUBLISHED' AND del_flag = '0') published_snapshot_count;
