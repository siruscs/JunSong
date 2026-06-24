#!/bin/bash
set -euo pipefail

RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

DEPLOY_DIR="$(cd "$(dirname "$0")" && pwd)"
COMPOSE_FILE="${DEPLOY_DIR}/docker-compose.yml"
ENV_FILE="${DEPLOY_DIR}/.env"
BACKUP_DIR="${DEPLOY_DIR}/backup/$(date +%Y%m%d)"
PROJECT_NAME="junsong"

BUSINESS_CONTAINERS=(
  junsong-gateway
  junsong-auth
  junsong-modules-system
  junsong-modules-gen
  junsong-modules-job
  junsong-modules-file
  junsong-modules-member
  junsong-modules-finance
  junsong-visual-monitor
)

CONFIG_BASES=(
  application
  redis
  datasource
  datasource-druid
  junsong-gateway
  junsong-auth
  junsong-system
  junsong-gen
  junsong-job
  junsong-file
  junsong-member
  junsong-finance
  junsong-monitor
)

log_info() { echo -e "${BLUE}[INFO]${NC} $1"; }
log_ok() { echo -e "${GREEN}[OK]${NC} $1"; }
log_warn() { echo -e "${YELLOW}[WARN]${NC} $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

usage() {
  cat <<EOF
用法: $0 [dev|prod|status|check] [--force]

命令:
  dev       切换运行环境到 dev
  prod      切换运行环境到 prod
  status    查看当前容器 profile 和健康状态
  check     检查 Nacos 中 dev/prod 配置是否齐全

选项:
  --force   即使目标环境和当前环境相同，也强制重建业务容器

示例:
  $0 status
  $0 check
  $0 dev
  $0 prod --force
EOF
}

run_compose() {
  if docker compose version >/dev/null 2>&1; then
    docker compose -f "$COMPOSE_FILE" "$@"
  else
    docker-compose -f "$COMPOSE_FILE" "$@"
  fi
}

load_env_value() {
  local key="$1"
  if [ -f "$ENV_FILE" ]; then
    grep -E "^${key}=" "$ENV_FILE" | tail -1 | cut -d= -f2- || true
  fi
}

current_env() {
  local from_env
  from_env="$(load_env_value SPRING_PROFILES_ACTIVE || true)"
  if [ -n "${from_env:-}" ]; then
    echo "$from_env"
    return 0
  fi
  for c in "${BUSINESS_CONTAINERS[@]}"; do
    if docker inspect "$c" >/dev/null 2>&1; then
      docker inspect "$c" --format '{{range .Config.Env}}{{println .}}{{end}}' | grep '^SPRING_PROFILES_ACTIVE=' | head -1 | cut -d= -f2-
      return 0
    fi
  done
  echo "unknown"
}

mysql_exec() {
  docker exec -i junsong-mysql sh -lc 'mysql --default-character-set=utf8mb4 -uroot -p"$MYSQL_ROOT_PASSWORD" -D "junsong-config" -N --batch --raw'
}

check_nacos_config() {
  local env_name="$1"
  local missing=0
  for base in "${CONFIG_BASES[@]}"; do
    local count
    count=$(printf "SELECT COUNT(*) FROM config_info WHERE data_id='${base}-${env_name}.yml' AND COALESCE(NULLIF(tenant_id,''),'public') IN ('public','default');\n" | mysql_exec | tail -1)
    if [ "${count:-0}" -eq 0 ]; then
      log_error "缺少 Nacos 配置: ${base}-${env_name}.yml"
      missing=$((missing + 1))
    else
      log_ok "Nacos 配置存在: ${base}-${env_name}.yml (${count})"
    fi
  done
  if [ "$missing" -ne 0 ]; then
    log_error "Nacos ${env_name} 配置缺失 ${missing} 项，停止切换"
    return 1
  fi
}

write_env_file() {
  local target_env="$1"
  mkdir -p "$BACKUP_DIR"
  if [ -f "$ENV_FILE" ]; then
    cp -a "$ENV_FILE" "$BACKUP_DIR/.env.before-switch-$(date +%H%M%S)"
  fi
  if grep -q '^SPRING_PROFILES_ACTIVE=' "$ENV_FILE" 2>/dev/null; then
    sed -i "s/^SPRING_PROFILES_ACTIVE=.*/SPRING_PROFILES_ACTIVE=${target_env}/" "$ENV_FILE"
  else
    printf '\nSPRING_PROFILES_ACTIVE=%s\n' "$target_env" >> "$ENV_FILE"
  fi
}

wait_health() {
  local name="$1"
  local port="$2"
  local path="http://127.0.0.1:${port}/actuator/health"
  for _ in $(seq 1 45); do
    local body
    body=$(curl -s "$path" 2>/dev/null || true)
    if echo "$body" | grep -q '"status":"UP"'; then
      log_ok "${name} 健康: UP"
      return 0
    fi
    sleep 2
  done
  log_warn "${name} 健康检查未确认 UP，请查看日志"
}

status() {
  log_info "当前 .env SPRING_PROFILES_ACTIVE: $(load_env_value SPRING_PROFILES_ACTIVE || true)"
  for c in "${BUSINESS_CONTAINERS[@]}"; do
    if docker inspect "$c" >/dev/null 2>&1; then
      local profile state
      profile=$(docker inspect "$c" --format '{{range .Config.Env}}{{println .}}{{end}}' | grep '^SPRING_PROFILES_ACTIVE=' | head -1 | cut -d= -f2- || true)
      state=$(docker inspect "$c" --format '{{.State.Status}}' 2>/dev/null || true)
      printf '%-30s %-10s %s\n' "$c" "$state" "$profile"
    else
      printf '%-30s %-10s %s\n' "$c" "missing" ""
    fi
  done
}

switch_env() {
  local target_env="$1"
  local force="$2"
  if [ ! -f "$COMPOSE_FILE" ]; then
    log_error "找不到 compose 文件: $COMPOSE_FILE"
    exit 1
  fi
  local before_env
  before_env="$(current_env)"
  log_info "当前环境: ${before_env}"
  log_info "目标环境: ${target_env}"
  check_nacos_config "$target_env"
  if [ "$before_env" = "$target_env" ] && [ "$force" != "true" ]; then
    log_warn "当前已经是 ${target_env}，无需切换。需要重建请加 --force"
    status
    exit 0
  fi
  mkdir -p "$BACKUP_DIR"
  cp -a "$COMPOSE_FILE" "$BACKUP_DIR/docker-compose.yml.before-switch-$(date +%H%M%S)"
  write_env_file "$target_env"
  log_info "开始重建业务容器"
  SPRING_PROFILES_ACTIVE="$target_env" run_compose up -d --force-recreate \
    junsong-gateway \
    junsong-auth \
    junsong-modules-system \
    junsong-modules-gen \
    junsong-modules-job \
    junsong-modules-file \
    junsong-modules-member \
    junsong-modules-finance \
    junsong-visual-monitor
  log_info "等待服务健康"
  wait_health junsong-gateway 8081
  wait_health junsong-auth 9200
  wait_health junsong-modules-system 9201
  wait_health junsong-modules-gen 9202
  wait_health junsong-modules-job 9203
  wait_health junsong-modules-file 9300
  wait_health junsong-modules-member 9206
  wait_health junsong-modules-finance 9205
  wait_health junsong-visual-monitor 9100
  status
  log_ok "运行环境已切换到 ${target_env}"
}

main() {
  if [ $# -lt 1 ]; then
    usage
    exit 1
  fi
  local cmd="$1"
  shift || true
  local force="false"
  for arg in "$@"; do
    case "$arg" in
      --force) force="true" ;;
      *) log_error "未知参数: $arg"; usage; exit 1 ;;
    esac
  done
  case "$cmd" in
    dev|prod) switch_env "$cmd" "$force" ;;
    status) status ;;
    check)
      log_info "检查 dev 配置"
      check_nacos_config dev
      log_info "检查 prod 配置"
      check_nacos_config prod
      ;;
    -h|--help|help) usage ;;
    *) log_error "未知命令: $cmd"; usage; exit 1 ;;
  esac
}

main "$@"
