#!/bin/bash
# =============================================
# JunSong Cloud - SQL 菜单部署脚本
# 用法: ./deploy-sql.sh <sql-file-or-dir> [dev|prod]
# 示例:
#   ./deploy-sql.sh sql/finance_operation_dashboard_menu.sql
#   ./deploy-sql.sh sql/  (执行目录下所有 .sql 文件)
# =============================================

set -e

ENV=${2:-dev}
PROJECT_ROOT="/Users/sirius/Documents/TRAE/JunSong-Cloud"
DOCKER_PATH="/Applications/Docker.app/Contents/Resources/bin/docker"
MYSQL_CONTAINER="junsong-mysql"
DB_NAME="junsong-cloud"
MYSQL_USER="root"
MYSQL_PASS="root_123"

export PATH="/Applications/Docker.app/Contents/Resources/bin:$PATH"

TARGET=$1

if [ -z "${TARGET}" ]; then
    echo "用法: ./deploy-sql.sh <sql-file-or-dir> [dev|prod]"
    echo "示例: ./deploy-sql.sh sql/finance_operation_dashboard_menu.sql"
    echo "      ./deploy-sql.sh sql/"
    exit 1
fi

echo "=========================================="
echo "  SQL 部署 - ${ENV}环境"
echo "=========================================="

cd ${PROJECT_ROOT}

apply_sql() {
    local file=$1
    echo ""
    echo "[SQL] 执行: $(basename ${file})"
    docker exec -i ${MYSQL_CONTAINER} mysql -u${MYSQL_USER} -p${MYSQL_PASS} --batch --raw \`${DB_NAME}\` < "${file}" 2>/dev/null
    if [ $? -eq 0 ]; then
        echo "  ✓ $(basename ${file}) 执行成功"
    else
        echo "  ✗ $(basename ${file}) 执行失败"
        return 1
    fi
}

if [ -d "${TARGET}" ]; then
    echo ""
    echo "目录模式：执行 ${TARGET} 下所有 .sql 文件"
    for f in "${TARGET}"/*.sql; do
        if [ -f "$f" ]; then
            apply_sql "$f"
        fi
    done
elif [ -f "${TARGET}" ]; then
    apply_sql "${TARGET}"
else
    echo "✗ 路径不存在: ${TARGET}"
    exit 1
fi

echo ""
echo "=========================================="
echo "  SQL 部署完成！"
echo "=========================================="
