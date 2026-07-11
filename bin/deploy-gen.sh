#!/bin/bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/deploy-common.sh"
deploy_backend_service "${1:-dev}" "junsong-modules-gen" "junsong-modules-gen" "junsong-modules/junsong-gen" \
  "junsong-modules/junsong-gen/target/junsong-modules-gen.jar" "docker/junsong/modules/gen/jar/junsong-modules-gen.jar" \
  "/home/junsong/junsong-modules-gen.jar" "/root/deploy/junsong/modules/gen/jar/junsong-modules-gen.jar"
