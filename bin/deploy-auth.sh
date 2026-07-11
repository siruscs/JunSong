#!/bin/bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/deploy-common.sh"
deploy_backend_service "${1:-dev}" "junsong-auth" "junsong-auth" "junsong-auth" \
  "junsong-auth/target/junsong-auth.jar" "docker/junsong/auth/jar/junsong-auth.jar" \
  "/home/junsong/junsong-auth.jar" "/root/deploy/junsong/auth/jar/junsong-auth.jar"
