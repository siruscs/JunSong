#!/bin/bash
# =============================================
# JunSong Cloud 一键更新部署脚本
# 用于编译代码、重构Docker镜像并重启服务
# =============================================
# 更新记录：
# 2026-06-02: 修复服务注册到Nacos问题，使用lb://前缀进行服务发现
#             修复验证码接口路由问题，确保/code路径由网关处理
#             添加member、finance业务模块支持
# 2026-06-05: 修复docker compose build缓存导致JAR文件未更新的问题
#             改用docker cp + restart替代docker compose build
#             前端文件通过volume挂载目录直接更新，无需重启容器
# =============================================

set -e

echo "=========================================="
echo "  JunSong Cloud 部署更新脚本"
echo "  Version: 2026-06-05"
echo "=========================================="

cd /Users/sirius/Documents/TRAE/JunSong-Cloud

echo ""
echo "[1/5] 编译后端代码..."
mvn clean package -DskipTests -q

if [ $? -eq 0 ]; then
    echo "✓ 后端编译成功"
else
    echo "✗ 后端编译失败"
    exit 1
fi

echo ""
echo "[2/5] 复制后端JAR包到Docker目录..."
cp junsong-gateway/target/junsong-gateway.jar docker/junsong/gateway/jar/junsong-gateway.jar
cp junsong-auth/target/junsong-auth.jar docker/junsong/auth/jar/junsong-auth.jar
cp junsong-modules/junsong-system/target/junsong-modules-system.jar docker/junsong/modules/system/jar/junsong-modules-system.jar
cp junsong-modules/junsong-gen/target/junsong-modules-gen.jar docker/junsong/modules/gen/jar/junsong-modules-gen.jar
cp junsong-modules/junsong-job/target/junsong-modules-job.jar docker/junsong/modules/job/jar/junsong-modules-job.jar
cp junsong-modules/junsong-file/target/junsong-modules-file.jar docker/junsong/modules/file/jar/junsong-modules-file.jar
cp junsong-modules/junsong-member/target/junsong-modules-member.jar docker/junsong/modules/member/jar/junsong-modules-member.jar
cp junsong-modules/junsong-finance/target/junsong-modules-finance.jar docker/junsong/modules/finance/jar/junsong-modules-finance.jar
echo "✓ JAR包复制成功"

echo ""
echo "[3/5] 编译前端代码..."
cd junsong-ui

if [ ! -d "node_modules" ]; then
    echo "安装前端依赖..."
    npm install
fi

npm run build:prod

if [ $? -eq 0 ]; then
    echo "✓ 前端编译成功"
else
    echo "✗ 前端编译失败"
    exit 1
fi

echo ""
echo "[4/5] 复制前端文件到Docker目录..."
cd ..
cp -r junsong-ui/dist/* docker/nginx/html/dist/
echo "✓ 前端文件复制成功（volume挂载目录，无需重启nginx）"

echo ""
echo "[5/5] 更新运行中的容器..."

cd docker

is_container_running() {
    docker ps --format '{{.Names}}' | grep -q "^${1}$"
}

declare -A SERVICES
SERVICES=(
    ["junsong-gateway"]="/home/junsong/junsong-gateway.jar docker/junsong/gateway/jar/junsong-gateway.jar"
    ["junsong-auth"]="/home/junsong/junsong-auth.jar docker/junsong/auth/jar/junsong-auth.jar"
    ["junsong-modules-system"]="/home/junsong/junsong-modules-system.jar docker/junsong/modules/system/jar/junsong-modules-system.jar"
    ["junsong-modules-gen"]="/home/junsong/junsong-modules-gen.jar docker/junsong/modules/gen/jar/junsong-modules-gen.jar"
    ["junsong-modules-job"]="/home/junsong/junsong-modules-job.jar docker/junsong/modules/job/jar/junsong-modules-job.jar"
    ["junsong-modules-file"]="/home/junsong/junsong-modules-file.jar docker/junsong/modules/file/jar/junsong-modules-file.jar"
    ["junsong-modules-member"]="/home/junsong/junsong-modules-member.jar docker/junsong/modules/member/jar/junsong-modules-member.jar"
    ["junsong-modules-finance"]="/home/junsong/junsong-modules-finance.jar docker/junsong/modules/finance/jar/junsong-modules-finance.jar"
)

NEED_COMPOSE_UP=false

for container in "${!SERVICES[@]}"; do
    read -r container_path host_path <<< "${SERVICES[$container]}"
    if is_container_running "${container}"; then
        echo "更新 ${container}..."
        docker cp "${host_path}" "${container}:${container_path}"
        docker restart "${container}"
        echo "✓ ${container} 已更新并重启"
    else
        echo "⚠ ${container} 容器未运行"
        NEED_COMPOSE_UP=true
    fi
done

if [ "$NEED_COMPOSE_UP" = true ]; then
    echo "存在未运行的容器，执行 docker compose up -d 启动所有服务..."
    docker compose up -d
fi

echo ""
echo "等待服务启动（60秒）..."
sleep 60

echo ""
echo "=========================================="
echo "  服务状态检查"
echo "=========================================="
docker compose ps

echo ""
echo "最近日志（各服务最新5行）："
docker compose logs --tail=5

echo ""
echo "=========================================="
echo "  部署完成！"
echo "=========================================="
echo ""
echo "访问地址："
echo "  前端：http://localhost"
echo "  网关：http://localhost:8080"
echo "  Nacos：http://localhost:8848/nacos"
echo ""
echo "Nacos默认账号：nacos"
echo "Nacos默认密码：nacos"
echo ""
echo "服务列表："
echo "  junsong-gateway     - 网关服务 [8080]"
echo "  junsong-auth        - 认证服务 [9200]"
echo "  junsong-modules-system - 系统模块 [9201]"
echo "  junsong-modules-gen    - 代码生成 [9202]"
echo "  junsong-modules-job    - 定时任务 [9203]"
echo "  junsong-modules-file   - 文件服务 [9300]"
echo "  junsong-modules-member - 会员模块 [9204]"
echo "  junsong-modules-finance - 财务模块 [9205]"
echo ""
echo "查看完整日志："
echo "  docker compose logs -f"
echo ""
echo "查看特定服务日志："
echo "  docker logs -f junsong-gateway"
echo "  docker logs -f junsong-auth"
echo "  docker logs -f junsong-modules-system"
