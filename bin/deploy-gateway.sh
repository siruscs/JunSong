#!/bin/bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/deploy-common.sh"
deploy_backend_service "${1:-dev}" "junsong-gateway" "junsong-gateway" "junsong-gateway" \
  "junsong-gateway/target/junsong-gateway.jar" "docker/junsong/gateway/jar/junsong-gateway.jar" \
  "/home/junsong/junsong-gateway.jar" "/root/deploy/junsong/gateway/jar/junsong-gateway.jar"
