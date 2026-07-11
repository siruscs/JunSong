#!/bin/bash

set -euo pipefail

PROJECT_ROOT="${PROJECT_ROOT:-/Users/sirius/Documents/TRAE/JunSong-Cloud}"
PROD_HOST="***PROD_HOST_REDACTED***"
PROD_USER="root"
PROD_SSH_KEY="${PROD_SSH_KEY:-/Users/sirius/.ssh/id_rsa}"
PROD_DEPLOY_DIR="${PROD_DEPLOY_DIR:-/root/deploy}"
DEPLOY_DRY_RUN="${DEPLOY_DRY_RUN:-0}"
DEPLOY_SKIP_BUILD="${DEPLOY_SKIP_BUILD:-0}"

log() { printf '%s\n' "$*"; }
die() { log "✗ $*" >&2; exit 1; }

print_command() {
    printf '[dry-run]'
    printf ' %q' "$@"
    printf '\n'
}

run_cmd() {
    if [ "${DEPLOY_DRY_RUN}" = "1" ]; then
        print_command "$@"
        return 0
    fi
    "$@"
}

ssh_options=(-i "${PROD_SSH_KEY}" -o BatchMode=yes -o ConnectTimeout=10)

prod_ssh() {
    if [ "${DEPLOY_DRY_RUN}" = "1" ]; then
        print_command ssh "${ssh_options[@]}" "${PROD_USER}@${PROD_HOST}" "$@"
        return 0
    fi
    ssh "${ssh_options[@]}" "${PROD_USER}@${PROD_HOST}" "$@"
}

prod_scp() {
    if [ "${DEPLOY_DRY_RUN}" = "1" ]; then
        print_command scp "${ssh_options[@]}" "$1" "${PROD_USER}@${PROD_HOST}:$2"
        return 0
    fi
    scp "${ssh_options[@]}" "$1" "${PROD_USER}@${PROD_HOST}:$2"
}

local_sha256() {
    shasum -a 256 "$1" | awk '{print $1}'
}

validate_environment() {
    case "$1" in
        dev|prod) ;;
        *) die "环境必须是 dev 或 prod，当前值: $1" ;;
    esac
}

wait_for_container() {
    local container=$1
    local runner=$2
    local attempt
    for attempt in 1 2 3 4 5 6; do
        if [ "${runner}" = "prod" ]; then
            if prod_ssh "docker inspect -f '{{.State.Running}}' '${container}' 2>/dev/null" | grep -q true; then
                return 0
            fi
        elif docker inspect -f '{{.State.Running}}' "${container}" 2>/dev/null | grep -q true; then
            return 0
        fi
        sleep 2
    done
    die "容器 ${container} 未能正常启动"
}

deploy_backend_service() {
    local env=$1
    local service_name=$2
    local container_name=$3
    local maven_module=$4
    local jar_path=$5
    local docker_jar_path=$6
    local container_dest=$7
    local remote_jar_path=$8

    validate_environment "${env}"
    cd "${PROJECT_ROOT}"

    log "=========================================="
    log "  ${service_name} 部署 - ${env} 环境"
    log "=========================================="

    if [ "${DEPLOY_SKIP_BUILD}" != "1" ]; then
        log "[1/4] 编译 ${service_name}"
        run_cmd mvn clean package -pl "${maven_module}" -am -DskipTests -q
    else
        log "[1/4] 跳过编译（DEPLOY_SKIP_BUILD=1）"
    fi

    if [ "${DEPLOY_DRY_RUN}" != "1" ]; then
        [ -f "${jar_path}" ] || die "构建产物不存在: ${jar_path}"
        mkdir -p "$(dirname "${docker_jar_path}")"
        cp "${jar_path}" "${docker_jar_path}"
    else
        print_command cp "${jar_path}" "${docker_jar_path}"
    fi

    if [ "${env}" = "dev" ]; then
        log "[2/4] 更新本地开发容器"
        if docker ps --format '{{.Names}}' | grep -qx "${container_name}"; then
            run_cmd docker cp "${docker_jar_path}" "${container_name}:${container_dest}"
            run_cmd docker restart "${container_name}"
        else
            run_cmd docker compose -f "${PROJECT_ROOT}/docker/docker-compose.yml" up -d "${service_name}"
        fi
        [ "${DEPLOY_DRY_RUN}" = "1" ] || wait_for_container "${container_name}" dev
        if [ "${DEPLOY_DRY_RUN}" = "1" ]; then
            log "✓ ${service_name} DEV DRY-RUN 计划生成完成（未复制、未重启）"
        else
            log "✓ ${service_name} DEV 部署完成"
        fi
        return 0
    fi

    local timestamp remote_tmp remote_backup_dir remote_backup local_hash
    timestamp=$(date '+%Y%m%d%H%M%S')
    remote_tmp="${PROD_DEPLOY_DIR}/.deploy-tmp/${service_name}-${timestamp}.jar"
    remote_backup_dir="${PROD_DEPLOY_DIR}/backup/$(date '+%Y%m%d')/${service_name}"
    remote_backup="${remote_backup_dir}/$(basename "${remote_jar_path}").before-${timestamp}"
    local_hash="dry-run"
    [ "${DEPLOY_DRY_RUN}" = "1" ] || local_hash=$(local_sha256 "${docker_jar_path}")

    log "[2/4] 上传到 PROD ${PROD_USER}@${PROD_HOST}"
    prod_ssh "mkdir -p '${PROD_DEPLOY_DIR}/.deploy-tmp' '${remote_backup_dir}' '$(dirname "${remote_jar_path}")'"
    prod_scp "${docker_jar_path}" "${remote_tmp}"

    log "[3/4] 备份并原子替换远程 JAR"
    log "[4/4] 强制重建、注入新 JAR、重启并校验"
    prod_ssh "set -Eeuo pipefail;
backup='${remote_backup}';
rollback() {
  trap - ERR;
  if [ -f \"\$backup\" ]; then
    cp -p \"\$backup\" '${remote_jar_path}';
    if docker inspect '${container_name}' >/dev/null 2>&1; then
      docker cp '${remote_jar_path}' '${container_name}:${container_dest}' >/dev/null;
      docker restart '${container_name}' >/dev/null;
    fi;
  fi;
};
trap rollback ERR;
if [ -f '${remote_jar_path}' ]; then cp -p '${remote_jar_path}' \"\$backup\"; fi;
mv '${remote_tmp}' '${remote_jar_path}';
test \"\$(sha256sum '${remote_jar_path}' | awk '{print \$1}')\" = '${local_hash}';
cd '${PROD_DEPLOY_DIR}';
compose_file=\$(docker inspect -f '{{ index .Config.Labels \"com.docker.compose.project.config_files\" }}' '${container_name}' 2>/dev/null || true);
compose_file=\${compose_file%%,*};
if [ -z \"\$compose_file\" ]; then if [ -f docker-compose.prod.yml ]; then compose_file=docker-compose.prod.yml; else compose_file=docker-compose.yml; fi; fi;
docker compose -f \"\$compose_file\" up -d --no-deps --force-recreate '${service_name}';
docker cp '${remote_jar_path}' '${container_name}:${container_dest}' >/dev/null;
docker restart '${container_name}' >/dev/null;
running=false;
for attempt in 1 2 3 4 5 6; do if [ \"\$(docker inspect -f '{{.State.Running}}' '${container_name}' 2>/dev/null)\" = true ]; then running=true; break; fi; sleep 2; done;
test \"\$running\" = true;
sleep 8;
test \"\$(docker inspect -f '{{.State.Running}}' '${container_name}' 2>/dev/null)\" = true;
test \"\$(docker exec '${container_name}' sha256sum '${container_dest}' | awk '{print \$1}')\" = '${local_hash}';
logs=\$(docker logs --tail=120 '${container_name}' 2>&1);
if printf '%s\n' \"\$logs\" | grep -Eq 'Application run failed|Error starting ApplicationContext|OutOfMemoryError'; then printf '%s\n' \"\$logs\" >&2; false; fi;
trap - ERR"

    if [ "${DEPLOY_DRY_RUN}" = "1" ]; then
        log "✓ ${service_name} PROD DRY-RUN 计划生成完成（未上传、未替换、未重启）"
    else
        log "✓ ${service_name} PROD 部署完成，SHA-256: ${local_hash}"
    fi
}
