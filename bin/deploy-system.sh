#!/bin/bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/deploy-common.sh"
deploy_backend_service "${1:-dev}" "junsong-modules-system" "junsong-modules-system" "junsong-modules/junsong-system" \
  "junsong-modules/junsong-system/target/junsong-modules-system.jar" "docker/junsong/modules/system/jar/junsong-modules-system.jar" \
  "/home/junsong/junsong-modules-system.jar" "/root/deploy/junsong/modules/system/jar/junsong-modules-system.jar"
