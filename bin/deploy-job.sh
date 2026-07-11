#!/bin/bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/deploy-common.sh"
deploy_backend_service "${1:-dev}" "junsong-modules-job" "junsong-modules-job" "junsong-modules/junsong-job" \
  "junsong-modules/junsong-job/target/junsong-modules-job.jar" "docker/junsong/modules/job/jar/junsong-modules-job.jar" \
  "/home/junsong/junsong-modules-job.jar" "/root/deploy/junsong/modules/job/jar/junsong-modules-job.jar"
