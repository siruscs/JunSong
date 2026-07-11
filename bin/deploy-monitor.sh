#!/bin/bash
set -euo pipefail
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/deploy-common.sh"
deploy_backend_service "${1:-dev}" "junsong-visual-monitor" "junsong-visual-monitor" "junsong-visual/junsong-monitor" \
  "junsong-visual/junsong-monitor/target/junsong-visual-monitor.jar" "docker/junsong/visual/monitor/jar/junsong-visual-monitor.jar" \
  "/home/junsong/junsong-visual-monitor.jar" "/root/deploy/junsong/visual/monitor/jar/junsong-visual-monitor.jar"
