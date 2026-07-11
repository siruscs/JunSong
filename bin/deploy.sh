#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/deploy-common.sh"
ENV="${1:-dev}"
validate_environment "${ENV}"

cd "${PROJECT_ROOT}"
log "[1/3] 一次性编译全部后端模块"
run_cmd mvn clean package -DskipTests -q

BACKEND_DEPLOYERS=(
  deploy-auth.sh deploy-gateway.sh deploy-system.sh deploy-gen.sh deploy-job.sh
  deploy-workflow.sh deploy-file.sh deploy-member.sh deploy-finance.sh deploy-open.sh
  deploy-monitor.sh
)
log "[2/3] 逐服务部署并校验"
for deployer in "${BACKEND_DEPLOYERS[@]}"; do
    DEPLOY_SKIP_BUILD=1 "${SCRIPT_DIR}/${deployer}" "${ENV}"
done

log "[3/3] 构建并部署 PC 前端"
"${SCRIPT_DIR}/deploy-ui.sh" "${ENV}"
if [ "${DEPLOY_DRY_RUN}" = "1" ]; then
    log "✓ JunSong Cloud ${ENV} 全量 DRY-RUN 计划生成完成（未执行部署）"
else
    log "✓ JunSong Cloud ${ENV} 全量部署完成"
fi
