#!/bin/bash
# =============================================
# JunSong Cloud 功能验证脚本
# 用于检查字典数据和多部门功能是否正常
# =============================================

echo "=========================================="
echo "  JunSong Cloud 功能验证"
echo "=========================================="

# 1. 检查字典数据
echo ""
echo "[1/3] 检查字典数据..."
echo "查询会员卡类型字典..."
docker exec -i junsong-mysql mysql -uroot -pchange-me-db-password -e "
USE junsong-cloud;
SELECT 
    dt.dict_name AS '字典名称',
    dt.dict_type AS '字典类型',
    dt.status AS '状态',
    COUNT(dd.dict_code) AS '数据条数'
FROM sys_dict_type dt
LEFT JOIN sys_dict_data dd ON dt.dict_type = dd.dict_type
WHERE dt.dict_type = 'mem_card_type'
GROUP BY dt.dict_id;
"

echo ""
echo "会员卡类型字典详情："
docker exec -i junsong-mysql mysql -uroot -pchange-me-db-password -e "
USE junsong-cloud;
SELECT 
    dict_label AS '类型名称',
    dict_value AS '类型值',
    dict_sort AS '排序',
    status AS '状态'
FROM sys_dict_data
WHERE dict_type = 'mem_card_type'
ORDER BY dict_sort;
"

# 2. 检查用户部门关联表
echo ""
echo "[2/3] 检查用户部门关联功能..."
echo "用户部门关联表结构："
docker exec -i junsong-mysql mysql -uroot -pchange-me-db-password -e "
USE junsong-cloud;
SHOW CREATE TABLE sys_user_dept\G
"

echo ""
echo "示例数据："
docker exec -i junsong-mysql mysql -uroot -pchange-me-db-password -e "
USE junsong-cloud;
SELECT 
    ud.user_dept_id AS 'ID',
    u.user_name AS '用户名',
    d.dept_name AS '部门名称',
    ud.status AS '状态'
FROM sys_user_dept ud
LEFT JOIN sys_user u ON ud.user_id = u.user_id
LEFT JOIN sys_dept d ON ud.dept_id = d.dept_id
LIMIT 10;
"

# 3. 检查Docker容器状态
echo ""
echo "[3/3] 检查Docker容器状态..."
docker compose -f /Users/sirius/Documents/TRAE/JunSong-Cloud/docker/docker-compose.yml ps

echo ""
echo "=========================================="
echo "  验证完成！"
echo "=========================================="
echo ""
echo "如果上述检查发现问题，请执行以下步骤："
echo ""
echo "1. 执行SQL脚本初始化字典数据："
echo "   docker exec -i junsong-mysql mysql -uroot -pchange-me-db-password < sql/init_mem_card_type.sql"
echo ""
echo "2. 重新编译并部署："
echo "   cd /Users/sirius/Documents/TRAE/JunSong-Cloud"
echo "   ./bin/deploy.sh"
echo ""
echo "3. 清理浏览器缓存并重新登录系统"
echo ""
