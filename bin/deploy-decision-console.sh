#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ENV="${1:-dev}"

SQL_FILES=(
  "sql/finance_operation_dashboard_menu.sql"
  "sql/finance_operating_reports_menu.sql"
  "sql/member_contribution_report_menu.sql"
)

for file in "${SQL_FILES[@]}"; do
    if [ -f "${SCRIPT_DIR}/../${file}" ]; then
        "${SCRIPT_DIR}/deploy-sql.sh" "${file}" "${ENV}"
    fi
done
"${SCRIPT_DIR}/deploy-finance.sh" "${ENV}"
"${SCRIPT_DIR}/deploy-member.sh" "${ENV}"
"${SCRIPT_DIR}/deploy-ui.sh" "${ENV}"

if [ "${DEPLOY_DRY_RUN:-0}" = "1" ]; then
    printf '✓ 经营决策台 %s DRY-RUN 计划生成完成（未执行部署）\n' "${ENV}"
else
    printf '✓ 经营决策台 %s 部署完成\n' "${ENV}"
fi
