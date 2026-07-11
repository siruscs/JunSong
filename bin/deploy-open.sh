#!/bin/bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/deploy-common.sh"
deploy_backend_service "${1:-dev}" "junsong-modules-open" "junsong-modules-open" "junsong-modules/junsong-open" \
  "junsong-modules/junsong-open/target/junsong-modules-open.jar" "docker/junsong/modules/open/jar/junsong-modules-open.jar" \
  "/home/junsong/junsong-modules-open.jar" "/root/deploy/junsong/modules/open/jar/junsong-modules-open.jar"
