#!/bin/bash
# =============================================
# JunSong Cloud - 安全清理菜单/权限缓存
# 只清 sys_menu:* key，不影响 sys_config/sys_dict 等其他缓存
# 用法: ./bin/clear-menu-cache.sh [dev|prod]
# =============================================

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/deploy-common.sh"

ENV="${1:-dev}"
REDIS_CONTAINER="junsong-redis"
validate_environment "${ENV}"

echo "=========================================="
echo "  清理菜单缓存 - ${ENV}环境"
echo "  (只清 sys_menu:* ，不影响其他缓存)"
echo "=========================================="

if [ "${ENV}" = "prod" ]; then
    echo "[PROD] 通过 SSH 清理远程 Redis 菜单缓存..."
    prod_ssh "set -o pipefail; docker exec '${REDIS_CONTAINER}' redis-cli --scan --pattern 'sys_menu:*' | xargs -r -n 100 docker exec -i '${REDIS_CONTAINER}' redis-cli DEL"
else
    echo "[DEV] 清理本地 Redis 菜单缓存..."
    KEYS=$(docker exec ${REDIS_CONTAINER} redis-cli --scan --pattern 'sys_menu:*')
    if [ -z "${KEYS}" ]; then
        echo "  无 sys_menu:* 缓存，无需清理"
    else
        echo "${KEYS}" | xargs docker exec -i ${REDIS_CONTAINER} redis-cli DEL
    fi
fi

echo ""
echo "✓ 菜单缓存已清理（用户刷新页面后会自动从数据库重新加载）"
echo ""
echo "注意: 此脚本只清 sys_menu:* 缓存。"
echo "      不要使用 FLUSHDB/FLUSHALL，那会误清 sys_config 等其他缓存。"
