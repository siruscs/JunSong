#!/bin/bash
# =============================================================================
# JunSong-Cloud 环境切换脚本
# 用法: ./switch-env.sh [dev|prod]
# =============================================================================

set -euo pipefail

# 颜色定义
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 项目根目录
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

# 需要修改的 bootstrap.yml 文件列表
BOOTSTRAP_FILES=(
  "junsong-auth/src/main/resources/bootstrap.yml"
  "junsong-gateway/src/main/resources/bootstrap.yml"
  "junsong-visual/junsong-monitor/src/main/resources/bootstrap.yml"
  "junsong-modules/junsong-system/src/main/resources/bootstrap.yml"
  "junsong-modules/junsong-member/src/main/resources/bootstrap.yml"
  "junsong-modules/junsong-finance/src/main/resources/bootstrap.yml"
  "junsong-modules/junsong-gen/src/main/resources/bootstrap.yml"
  "junsong-modules/junsong-job/src/main/resources/bootstrap.yml"
  "junsong-modules/junsong-file/src/main/resources/bootstrap.yml"
)

# docker-compose.yml 文件
DOCKER_COMPOSE_FILE="docker/docker-compose.yml"

# 日志函数
log_info()  { echo -e "${BLUE}[INFO]${NC}  $1"; }
log_ok()    { echo -e "${GREEN}[OK]${NC}    $1"; }
log_warn()  { echo -e "${YELLOW}[WARN]${NC}  $1"; }
log_error() { echo -e "${RED}[ERROR]${NC} $1"; }

# 打印用法
usage() {
  echo "用法: $0 [dev|prod]"
  echo ""
  echo "参数:"
  echo "  dev   - 切换到开发环境"
  echo "  prod  - 切换到生产环境"
  echo ""
  echo "示例:"
  echo "  $0 dev    # 切换到开发环境"
  echo "  $0 prod   # 切换到生产环境"
  exit 1
}

# 检测当前环境
detect_current_env() {
  local first_file="${PROJECT_DIR}/${BOOTSTRAP_FILES[0]}"
  if [ ! -f "$first_file" ]; then
    log_error "找不到文件: $first_file"
    log_error "请确认项目根目录是否正确: $PROJECT_DIR"
    exit 1
  fi

  if grep -q "active: dev" "$first_file" 2>/dev/null; then
    echo "dev"
  elif grep -q "active: prod" "$first_file" 2>/dev/null; then
    echo "prod"
  else
    echo "unknown"
  fi
}

# 替换文件中的环境标识
replace_in_file() {
  local file="$1"
  local from_env="$2"
  local to_env="$3"
  local full_path="${PROJECT_DIR}/${file}"

  if [ ! -f "$full_path" ]; then
    log_warn "文件不存在，跳过: $file"
    return 0
  fi

  # macOS sed 需要 -i '' 参数
  if [[ "$OSTYPE" == "darwin"* ]]; then
    sed -i '' "s/-${from_env}\.yml/-${to_env}.yml/g" "$full_path"
    sed -i '' "s/active: ${from_env}/active: ${to_env}/g" "$full_path"
    sed -i '' "s/active=${from_env}/active=${to_env}/g" "$full_path"
  else
    sed -i "s/-${from_env}\.yml/-${to_env}.yml/g" "$full_path"
    sed -i "s/active: ${from_env}/active: ${to_env}/g" "$full_path"
    sed -i "s/active=${from_env}/active=${to_env}/g" "$full_path"
  fi
}

# 打印切换后的结果
print_result() {
  local target_env="$1"
  local success_count=0
  local skip_count=0

  echo ""
  log_info "===== 环境切换结果 ====="
  echo ""

  log_info "bootstrap.yml 文件:"
  for file in "${BOOTSTRAP_FILES[@]}"; do
    local full_path="${PROJECT_DIR}/${file}"
    if [ -f "$full_path" ]; then
      local active_line
      active_line=$(grep "active:" "$full_path" | head -1 | xargs)
      log_ok "${file}"
      log_info "  ${active_line}"
      ((success_count++))
    else
      log_warn "${file} (文件不存在)"
      ((skip_count++))
    fi
  done

  echo ""
  log_info "docker-compose.yml 文件:"
  local docker_file="${PROJECT_DIR}/${DOCKER_COMPOSE_FILE}"
  if [ -f "$docker_file" ]; then
    local env_vars
    env_vars=$(grep "spring.profiles.active" "$docker_file" | head -1 | xargs)
    log_ok "${DOCKER_COMPOSE_FILE}"
    log_info "  ${env_vars}"
    ((success_count++))
  else
    log_warn "${DOCKER_COMPOSE_FILE} (文件不存在)"
    ((skip_count++))
  fi

  echo ""
  log_ok "成功切换 ${success_count} 个文件，跳过 ${skip_count} 个文件"
  log_ok "当前环境已切换为: ${GREEN}${target_env}${NC}"
}

# 主流程
main() {
  if [ $# -ne 1 ]; then
    usage
  fi

  local target_env="$1"

  # 验证参数
  if [[ "$target_env" != "dev" && "$target_env" != "prod" ]]; then
    log_error "无效的环境参数: $target_env"
    echo ""
    usage
  fi

  # 检测当前环境
  local current_env
  current_env=$(detect_current_env)

  echo ""
  log_info "===== JunSong-Cloud 环境切换 ====="
  echo ""
  log_info "当前环境: ${current_env}"
  log_info "目标环境: ${target_env}"
  echo ""

  # 如果当前环境和目标环境相同，提示用户
  if [ "$current_env" == "$target_env" ]; then
    log_warn "当前已经是 ${target_env} 环境，无需切换"
    echo ""
    log_info "如需强制刷新，可先切换到另一个环境再切回来"
    exit 0
  fi

  # 确认提示
  read -p "确认切换到 ${target_env} 环境？[y/N] " -r confirm
  if [[ ! "$confirm" =~ ^[Yy]$ ]]; then
    log_info "已取消切换"
    exit 0
  fi

  echo ""
  log_info "开始切换环境..."

  # 替换所有 bootstrap.yml 文件
  for file in "${BOOTSTRAP_FILES[@]}"; do
    replace_in_file "$file" "$current_env" "$target_env"
  done

  # 替换 docker-compose.yml
  replace_in_file "$DOCKER_COMPOSE_FILE" "$current_env" "$target_env"

  # 打印结果
  print_result "$target_env"

  echo ""
  log_warn "提示: 切换完成后，请重启相关服务以应用新配置"
  echo ""
}

main "$@"
