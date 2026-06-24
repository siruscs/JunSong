#!/bin/bash
# =============================================
# JunSong Cloud - PC端前端部署脚本
# 用法: ./deploy-ui.sh [dev|prod]
# =============================================

set -e

ENV=${1:-dev}
PROJECT_ROOT="/Users/sirius/Documents/TRAE/JunSong-Cloud"
UI_DIR="${PROJECT_ROOT}/junsong-ui-v3"
CONTAINER_NAME="junsong-nginx"
CONTAINER_DEST="/home/junsong/projects/junsong-ui/"

echo "=========================================="
echo "  PC端前端部署 - ${ENV}环境"
echo "=========================================="

cd ${UI_DIR}

# 检查依赖
echo ""
echo "[1/3] 检查依赖..."
if [ ! -d "node_modules" ]; then
    echo "安装前端依赖..."
    npm install
fi
echo "✓ 依赖检查完成"

# 编译
echo ""
echo "[2/3] 编译前端代码..."
if [ "${ENV}" = "prod" ] && npm run | grep -q "build:prod"; then
    npm run build:prod
elif [ "${ENV}" != "prod" ] && npm run | grep -q "build:dev"; then
    npm run build:dev
else
    npm run build
fi

if [ $? -ne 0 ]; then
    echo "✗ 编译失败"
    exit 1
fi
echo "✓ 编译成功"

# 更新容器
echo ""
echo "[3/3] 更新容器..."

if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    docker cp ${UI_DIR}/dist/. ${CONTAINER_NAME}:${CONTAINER_DEST}
    # nginx通过volume挂载，无需重启
    echo "✓ 前端文件已更新（nginx自动加载，无需重启）"
else
    echo "⚠ 容器 ${CONTAINER_NAME} 未运行，尝试启动..."
    cd ${PROJECT_ROOT}/docker
    if [ "${ENV}" = "prod" ]; then
        docker compose -f docker-compose.prod.yml up -d ${CONTAINER_NAME}
    else
        docker compose up -d ${CONTAINER_NAME}
    fi
fi

echo ""
echo "=========================================="
echo "  部署完成！"
echo "=========================================="
echo ""
echo "访问地址: http://localhost"
echo ""
echo "如需强制刷新浏览器缓存: Ctrl+Shift+R (Windows/Linux) 或 Cmd+Shift+R (Mac)"
