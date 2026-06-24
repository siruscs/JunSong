#!/bin/bash
# =============================================
# JunSong Cloud 快速更新脚本
# 专门用于快速编译并重启后端服务
# =============================================

echo "=========================================="
echo "  JunSong Cloud 快速更新"
echo "=========================================="

cd /Users/sirius/Documents/TRAE/JunSong-Cloud

echo ""
echo "[1/3] 编译打包..."
mvn clean package -DskipTests

if [ $? -ne 0 ]; then
    echo "✗ 编译失败"
    exit 1
fi
echo "✓ 编译成功"

echo ""
echo "[2/3] 复制JAR文件并更新容器..."
cp junsong-gateway/target/junsong-gateway.jar docker/junsong/gateway/jar/junsong-gateway.jar
cp junsong-auth/target/junsong-auth.jar docker/junsong/auth/jar/junsong-auth.jar
cp junsong-modules/junsong-system/target/junsong-modules-system.jar docker/junsong/modules/system/jar/junsong-modules-system.jar
cp junsong-modules/junsong-member/target/junsong-modules-member.jar docker/junsong/modules/member/jar/junsong-modules-member.jar
cp junsong-modules/junsong-finance/target/junsong-modules-finance.jar docker/junsong/modules/finance/jar/junsong-modules-finance.jar
cp junsong-modules/junsong-workflow/target/junsong-modules-workflow.jar docker/junsong/modules/workflow/jar/junsong-modules-workflow.jar

cd docker

docker cp junsong/gateway/jar/junsong-gateway.jar junsong-gateway:/home/junsong/junsong-gateway.jar
docker cp junsong/auth/jar/junsong-auth.jar junsong-auth:/home/junsong/junsong-auth.jar
docker cp junsong/modules/system/jar/junsong-modules-system.jar junsong-modules-system:/home/junsong/junsong-modules-system.jar
docker cp junsong/modules/member/jar/junsong-modules-member.jar junsong-modules-member:/home/junsong/junsong-modules-member.jar
docker cp junsong/modules/finance/jar/junsong-modules-finance.jar junsong-modules-finance:/home/junsong/junsong-modules-finance.jar
docker cp junsong/modules/workflow/jar/junsong-modules-workflow.jar junsong-modules-workflow:/home/junsong/junsong-modules-workflow.jar

echo "✓ JAR文件复制完成"

echo ""
echo "[3/3] 重启服务..."
docker restart junsong-gateway junsong-auth junsong-modules-system junsong-modules-member junsong-modules-finance junsong-modules-workflow

echo ""
echo "=========================================="
echo "  更新完成！"
echo "=========================================="
echo ""
echo "服务状态："
docker compose ps | grep -E "(junsong-auth|junsong-gateway|junsong-modules-system|junsong-modules-member|junsong-modules-finance|junsong-modules-workflow)"
echo ""
echo "最近日志："
docker compose logs --tail=10 junsong-auth
echo ""
echo "建议：清理浏览器缓存后重新登录系统"
