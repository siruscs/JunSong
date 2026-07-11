#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/deploy-common.sh"

TARGET="${1:-}"
ENV="${2:-dev}"
MYSQL_CONTAINER="${MYSQL_CONTAINER:-junsong-mysql}"
DB_NAME="${DB_NAME:-junsong-cloud}"
MYSQL_USER="${MYSQL_USER:-root}"
MYSQL_PASS="${MYSQL_PASS:-root_123}"

[ -n "${TARGET}" ] || die "用法: ./deploy-sql.sh <sql-file-or-dir> [dev|prod]"
validate_environment "${ENV}"
cd "${PROJECT_ROOT}"

apply_sql() {
    local file=$1
    log "[SQL] 执行: $(basename "${file}")"
    if [ "${DEPLOY_DRY_RUN}" = "1" ]; then
        if [ "${ENV}" = "prod" ]; then
            print_command ssh "${PROD_USER}@${PROD_HOST}" docker exec -i "${MYSQL_CONTAINER}" mysql "-u${MYSQL_USER}" "-p***" "${DB_NAME}" '<' "${file}"
        else
            print_command docker exec -i "${MYSQL_CONTAINER}" mysql "-u${MYSQL_USER}" "-p***" "${DB_NAME}" '<' "${file}"
        fi
        return 0
    fi
    if [ "${ENV}" = "prod" ]; then
        prod_ssh "docker exec -i '${MYSQL_CONTAINER}' mysql '-u${MYSQL_USER}' '-p${MYSQL_PASS}' --batch --raw '${DB_NAME}'" < "${file}"
    else
        docker exec -i "${MYSQL_CONTAINER}" mysql "-u${MYSQL_USER}" "-p${MYSQL_PASS}" --batch --raw "${DB_NAME}" < "${file}"
    fi
    log "  ✓ $(basename "${file}")"
}

if [ "${ENV}" = "prod" ]; then
    backup="${PROD_DEPLOY_DIR}/backup/$(date '+%Y%m%d')/mysql/${DB_NAME}-before-$(date '+%Y%m%d%H%M%S').sql.gz"
    prod_ssh "set -e; set -o pipefail; mkdir -p '$(dirname "${backup}")'; docker exec '${MYSQL_CONTAINER}' mysqldump '-u${MYSQL_USER}' '-p${MYSQL_PASS}' --single-transaction '${DB_NAME}' | gzip > '${backup}'; test -s '${backup}'; gzip -t '${backup}'"
fi

if [ -d "${TARGET}" ]; then
    found=0
    for file in "${TARGET}"/*.sql; do
        [ -f "${file}" ] || continue
        found=1
        apply_sql "${file}"
    done
    [ "${found}" = "1" ] || die "目录中没有 SQL 文件: ${TARGET}"
elif [ -f "${TARGET}" ]; then
    apply_sql "${TARGET}"
else
    die "路径不存在: ${TARGET}"
fi

if [ "${DEPLOY_DRY_RUN}" = "1" ]; then
    log "✓ SQL ${ENV} DRY-RUN 计划生成完成（未执行 SQL）"
else
    log "✓ SQL ${ENV} 部署完成"
fi
