#!/bin/bash
# 将峻松 Nacos 配置导入 MySQL（junsong-config 库）
set -e
cd "$(dirname "$0")/.."

MYSQL_CONTAINER="${MYSQL_CONTAINER:-junsong-mysql}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASS="${MYSQL_PASS:-change-me-db-password}"
MYSQL_DB="${MYSQL_DB:-junsong-config}"

echo "==> 检查 MySQL 容器 ${MYSQL_CONTAINER} ..."
docker ps --format '{{.Names}}' | grep -q "^${MYSQL_CONTAINER}$" || {
  echo "错误: 容器 ${MYSQL_CONTAINER} 未运行"
  exit 1
}

if ! docker exec "${MYSQL_CONTAINER}" mysql -u"${MYSQL_USER}" -p"${MYSQL_PASS}" -N -e \
  "SELECT 1 FROM information_schema.tables WHERE table_schema='${MYSQL_DB}' AND table_name='config_info' LIMIT 1;" 2>/dev/null | grep -q 1; then
  echo "==> 未找到 config_info 表，先导入 Nacos 表结构 (sql/ry_config_20260311.sql) ..."
  echo "    注意: 该脚本会 DROP 并重建 junsong-config 库，仅建议在首次初始化时使用。"
  read -r -p "是否继续? [y/N] " ans
  if [[ "${ans}" =~ ^[Yy]$ ]]; then
    docker exec -i "${MYSQL_CONTAINER}" mysql -u"${MYSQL_USER}" -p"${MYSQL_PASS}" < sql/ry_config_20260311.sql
  else
    echo "已取消。请手动创建 Nacos 表结构后再执行本脚本。"
    exit 1
  fi
fi

echo "==> 导入峻松业务配置 (sql/junsong_nacos_config.sql) ..."
docker exec -i "${MYSQL_CONTAINER}" mysql -u"${MYSQL_USER}" -p"${MYSQL_PASS}" "${MYSQL_DB}" < sql/junsong_nacos_config.sql

echo "==> 修复控制台命名空间 (sql/nacos_console_fix.sql) ..."
docker exec -i "${MYSQL_CONTAINER}" mysql -u"${MYSQL_USER}" -p"${MYSQL_PASS}" "${MYSQL_DB}" < sql/nacos_console_fix.sql

echo "==> 当前配置列表:"
docker exec "${MYSQL_CONTAINER}" mysql -u"${MYSQL_USER}" -p"${MYSQL_PASS}" -N -e \
  "SELECT data_id FROM \`${MYSQL_DB}\`.config_info ORDER BY data_id;" 2>/dev/null

echo ""
echo "==> 重启 Nacos 使配置生效 ..."
docker restart junsong-nacos 2>/dev/null || echo "请手动执行: docker restart junsong-nacos"
echo ""
echo "完成。控制台访问: http://localhost:8848/index.html"
echo "命名空间请选择: public（默认）"
