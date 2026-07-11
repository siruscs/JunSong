#!/bin/bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/deploy-common.sh"
deploy_backend_service "${1:-dev}" "junsong-modules-workflow" "junsong-modules-workflow" "junsong-modules/junsong-workflow" \
  "junsong-modules/junsong-workflow/target/junsong-modules-workflow.jar" "docker/junsong/modules/workflow/jar/junsong-modules-workflow.jar" \
  "/home/junsong/junsong-modules-workflow.jar" "/root/deploy/junsong/modules/workflow/jar/junsong-modules-workflow.jar"
