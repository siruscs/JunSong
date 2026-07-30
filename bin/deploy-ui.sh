#!/bin/bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIR}/deploy-common.sh"

ENV="${1:-dev}"
validate_environment "${ENV}"
UI_DIR="${PROJECT_ROOT}/junsong-ui-v3"
CONTAINER_NAME="junsong-nginx"
CONTAINER_DEST="/home/junsong/projects/junsong-ui"

cd "${UI_DIR}"
if [ "${DEPLOY_SKIP_BUILD}" != "1" ]; then
    [ -d node_modules ] || run_cmd npm install
    if [ "${ENV}" = "prod" ] && npm run | grep -q 'build:prod'; then
        run_cmd npm run build:prod
    elif [ "${ENV}" = "dev" ] && npm run | grep -q 'build:dev'; then
        run_cmd npm run build:dev
    else
        run_cmd npm run build
    fi
fi

if [ "${DEPLOY_DRY_RUN}" != "1" ]; then
    [ -f dist/index.html ] || die "前端产物不存在: ${UI_DIR}/dist/index.html"
fi

if [ "${ENV}" = "dev" ]; then
    run_cmd docker exec "${CONTAINER_NAME}" sh -c "rm -rf '${CONTAINER_DEST:?}'/*"
    run_cmd docker cp "${UI_DIR}/dist/." "${CONTAINER_NAME}:${CONTAINER_DEST}/"
    if [ "${DEPLOY_DRY_RUN}" = "1" ]; then
        log "✓ 前端 DEV DRY-RUN 计划生成完成（未复制）"
    else
        log "✓ 前端 DEV 部署完成"
    fi
    exit 0
fi

REMOTE_DIST="${PROD_DEPLOY_DIR}/nginx/html/dist"
timestamp=$(date '+%Y%m%d%H%M%S')
archive="/tmp/junsong-ui-${timestamp}.tar.gz"
remote_archive="${PROD_DEPLOY_DIR}/.deploy-tmp/junsong-ui-${timestamp}.tar.gz"
backup_dir="${PROD_DEPLOY_DIR}/backup/$(date '+%Y%m%d')/ui-${timestamp}"

run_cmd tar -C "${UI_DIR}/dist" -czf "${archive}" .
prod_ssh "mkdir -p '${PROD_DEPLOY_DIR}/.deploy-tmp' '$(dirname "${REMOTE_DIST}")' '$(dirname "${backup_dir}")'"
prod_scp "${archive}" "${remote_archive}"
local_hash="dry-run"
[ "${DEPLOY_DRY_RUN}" = "1" ] || local_hash=$(local_sha256 "${UI_DIR}/dist/index.html")
prod_ssh "set -Eeuo pipefail;
cd '${PROD_DEPLOY_DIR}';
compose_file=\$(docker inspect -f '{{ index .Config.Labels \"com.docker.compose.project.config_files\" }}' '${CONTAINER_NAME}' 2>/dev/null || true);
compose_file=\${compose_file%%,*};
if [ -z \"\$compose_file\" ]; then if [ -f docker-compose.prod.yml ]; then compose_file=docker-compose.prod.yml; else compose_file=docker-compose.yml; fi; fi;
rollback() {
  trap - ERR;
  if [ -d '${backup_dir}' ]; then
    rm -rf '${REMOTE_DIST}';
    mv '${backup_dir}' '${REMOTE_DIST}';
    cd '${PROD_DEPLOY_DIR}';
    docker compose -f \"\$compose_file\" up -d --no-deps --force-recreate '${CONTAINER_NAME}';
  fi;
};
trap rollback ERR;
if [ -d '${REMOTE_DIST}' ]; then mv '${REMOTE_DIST}' '${backup_dir}'; fi;
mkdir -p '${REMOTE_DIST}';
tar -xzf '${remote_archive}' -C '${REMOTE_DIST}';
rm -f '${remote_archive}';
test \"\$(sha256sum '${REMOTE_DIST}/index.html' | awk '{print \$1}')\" = '${local_hash}';
cd '${PROD_DEPLOY_DIR}';
docker compose -f \"\$compose_file\" up -d --no-deps --force-recreate '${CONTAINER_NAME}';
running=false;
for attempt in 1 2 3 4 5 6; do if [ \"\$(docker inspect -f '{{.State.Running}}' '${CONTAINER_NAME}' 2>/dev/null)\" = true ]; then running=true; break; fi; sleep 2; done;
test \"\$running\" = true;
test \"\$(docker exec '${CONTAINER_NAME}' sha256sum '${CONTAINER_DEST}/index.html' | awk '{print \$1}')\" = '${local_hash}';
docker exec '${CONTAINER_NAME}' nginx -t;
trap - ERR"

run_cmd rm -f "${archive}"
if [ "${DEPLOY_DRY_RUN}" = "1" ]; then
    log "✓ 前端 PROD DRY-RUN 计划生成完成（未上传、未替换、未重启）"
else
    log "✓ 前端 PROD 部署完成 (${PROD_HOST})"
fi
