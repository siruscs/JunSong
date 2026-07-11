#!/bin/bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/deploy-common.sh"
deploy_backend_service "${1:-dev}" "junsong-modules-member" "junsong-modules-member" "junsong-modules/junsong-member" \
  "junsong-modules/junsong-member/target/junsong-modules-member.jar" "docker/junsong/modules/member/jar/junsong-modules-member.jar" \
  "/home/junsong/junsong-modules-member.jar" "/root/deploy/junsong/modules/member/jar/junsong-modules-member.jar"
