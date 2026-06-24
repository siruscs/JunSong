#!/bin/bash
# =============================================
# JunSong Cloud - 会员模块部署脚本
# 用法: ./deploy-member.sh [dev|prod]
# =============================================

set -e

ENV=${1:-dev}
PROJECT_ROOT="/Users/sirius/Documents/TRAE/JunSong-Cloud"
SERVICE_NAME="junsong-modules-member"
CONTAINER_NAME="junsong-modules-member"
JAR_NAME="junsong-modules-member.jar"
JAR_PATH="junsong-modules/junsong-member/target/junsong-modules-member.jar"
DOCKER_JAR_PATH="docker/junsong/modules/member/jar/junsong-modules-member.jar"
CONTAINER_DEST="/home/junsong/junsong-modules-member.jar"

echo "=========================================="
echo "  会员模块部署 - ${ENV}环境"
echo "=========================================="

cd ${PROJECT_ROOT}

# 编译
echo ""
echo "[1/3] 编译 ${SERVICE_NAME}..."
mvn clean package -pl junsong-modules/junsong-member -am -DskipTests -q

if [ $? -ne 0 ]; then
    echo "✗ 编译失败"
    exit 1
fi
echo "✓ 编译成功"

# 复制JAR
echo ""
echo "[2/3] 复制JAR文件..."
cp ${JAR_PATH} ${DOCKER_JAR_PATH}
echo "✓ JAR文件复制完成"

# 更新容器
echo ""
echo "[3/3] 更新容器..."

if docker ps --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    docker cp ${DOCKER_JAR_PATH} ${CONTAINER_NAME}:${CONTAINER_DEST}
    docker restart ${CONTAINER_NAME}
    echo "✓ ${SERVICE_NAME} 已更新并重启"
else
    echo "⚠ 容器 ${CONTAINER_NAME} 未运行，尝试启动..."
    cd docker
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
echo "查看日志: docker logs -f ${CONTAINER_NAME}"
