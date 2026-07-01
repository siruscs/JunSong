#!/bin/bash
# =============================================
# JunSong Cloud - 开放平台模块部署脚本
# 用法: ./deploy-open.sh [dev|prod]
# =============================================

set -e

ENV=${1:-dev}
PROJECT_ROOT="/Users/sirius/Documents/TRAE/JunSong-Cloud"
SERVICE_NAME="junsong-modules-open"
CONTAINER_NAME="junsong-modules-open"
JAR_NAME="junsong-modules-open.jar"
JAR_PATH="junsong-modules/junsong-open/target/junsong-modules-open.jar"
DOCKER_JAR_PATH="docker/junsong/modules/open/jar/junsong-modules-open.jar"
CONTAINER_DEST="/home/junsong/junsong-modules-open.jar"
IMAGE_NAME="junsong-modules-open:latest"
NETWORK_NAME="docker_default"

echo "=========================================="
echo "  开放平台模块部署 - ${ENV}环境"
echo "=========================================="

cd ${PROJECT_ROOT}

# 编译
echo ""
echo "[1/3] 编译 ${SERVICE_NAME}..."
mvn clean package -pl junsong-modules/junsong-open -am -DskipTests -q

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
    # open 容器不在 docker-compose.yml 中，需要手动 docker run 启动
    docker run -d \
        --name ${CONTAINER_NAME} \
        --network ${NETWORK_NAME} \
        -p 9208:9208 \
        -e SPRING_PROFILES_ACTIVE=${ENV} \
        -e NACOS_PASSWORD=nacos \
        -v ${DOCKER_JAR_PATH}:${CONTAINER_DEST} \
        ${IMAGE_NAME}
    echo "✓ ${SERVICE_NAME} 已启动"
fi

echo ""
echo "=========================================="
echo "  部署完成！"
echo "=========================================="
echo ""
echo "服务端口: 9208"
echo "查看日志: docker logs -f ${CONTAINER_NAME}"