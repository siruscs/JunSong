#!/bin/bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/deploy-common.sh"
deploy_backend_service "${1:-dev}" "junsong-modules-finance" "junsong-modules-finance" "junsong-modules/junsong-finance" \
  "junsong-modules/junsong-finance/target/junsong-modules-finance.jar" "docker/junsong/modules/finance/jar/junsong-modules-finance.jar" \
  "/home/junsong/junsong-modules-finance.jar" "/root/deploy/junsong/modules/finance/jar/junsong-modules-finance.jar"
