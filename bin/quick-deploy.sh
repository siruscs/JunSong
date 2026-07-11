#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/deploy-common.sh"
ENV="${1:-dev}"
validate_environment "${ENV}"

cd "${PROJECT_ROOT}"
run_cmd mvn clean package -DskipTests -q

BACKEND_DEPLOYERS=(
  deploy-auth.sh deploy-gateway.sh deploy-system.sh deploy-gen.sh deploy-job.sh
  deploy-workflow.sh deploy-file.sh deploy-member.sh deploy-finance.sh deploy-open.sh
)
for deployer in "${BACKEND_DEPLOYERS[@]}"; do
    DEPLOY_SKIP_BUILD=1 "${SCRIPT_DIR}/${deployer}" "${ENV}"
done

if [ "${DEPLOY_DRY_RUN}" = "1" ]; then
    log "✓ JunSong Cloud ${ENV} 后端 DRY-RUN 计划生成完成（未执行部署）"
else
    log "✓ JunSong Cloud ${ENV} 后端快速部署完成"
fi
