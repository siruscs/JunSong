#!/bin/bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/deploy-common.sh"
deploy_backend_service "${1:-dev}" "junsong-modules-file" "junsong-modules-file" "junsong-modules/junsong-file" \
  "junsong-modules/junsong-file/target/junsong-modules-file.jar" "docker/junsong/modules/file/jar/junsong-modules-file.jar" \
  "/home/junsong/junsong-modules-file.jar" "/root/deploy/junsong/modules/file/jar/junsong-modules-file.jar"
