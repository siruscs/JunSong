#!/bin/bash
# =============================================
# JunSong Cloud - 经营决策台一键部署
# 部署范围: finance + member + frontend + SQL 菜单
# 用法: ./deploy-decision-console.sh [dev|prod]
# =============================================

set -e

ENV=${1:-dev}
PROJECT_ROOT="/Users/sirius/Documents/TRAE/JunSong-Cloud"
export PATH="/Applications/Docker.app/Contents/Resources/bin:$PATH"

echo "=========================================="
echo "  经营决策台一键部署 - ${ENV}环境"
echo "  $(date '+%Y-%m-%d %H:%M:%S')"
echo "=========================================="

cd ${PROJECT_ROOT}

# Step 1: SQL 菜单部署
echo ""
echo "[1/4] 部署菜单 SQL..."
SQL_FILES=(
    "sql/finance_operation_dashboard_menu.sql"
    "sql/finance_operating_reports_menu.sql"
    "sql/member_contribution_report_menu.sql"
)
for f in "${SQL_FILES[@]}"; do
    if [ -f "$f" ]; then
        echo "  执行: $(basename $f)"
        docker exec -i junsong-mysql mysql -uroot -proot_123 --batch --raw \`junsong-cloud\` < "$f" 2>/dev/null
        echo "  ✓ $(basename $f)"
    else
        echo "  ⚠ 跳过: $f 不存在"
    fi
done
echo "✓ SQL 部署完成"

# Step 2: 后端编译
echo ""
echo "[2/4] 编译后端模块..."
echo "  编译 finance + member..."
mvn clean package -pl junsong-modules/junsong-finance,junsong-modules/junsong-member -am -DskipTests -q

if [ $? -ne 0 ]; then
    echo "✗ 编译失败"
    exit 1
fi
echo "✓ 编译成功"

# Step 3: 复制 JAR 并重启后端容器
echo ""
echo "[3/4] 更新后端容器..."
cp junsong-modules/junsong-finance/target/junsong-modules-finance.jar docker/junsong/modules/finance/jar/junsong-modules-finance.jar
cp junsong-modules/junsong-member/target/junsong-modules-member.jar docker/junsong/modules/member/jar/junsong-modules-member.jar

docker cp docker/junsong/modules/finance/jar/junsong-modules-finance.jar junsong-modules-finance:/home/junsong/junsong-modules-finance.jar
docker cp docker/junsong/modules/member/jar/junsong-modules-member.jar junsong-modules-member:/home/junsong/junsong-modules-member.jar

docker restart junsong-modules-finance junsong-modules-member
echo "✓ finance + member 容器已重启"

# Step 4: 前端构建与部署
echo ""
echo "[4/4] 构建并部署前端..."
cd junsong-ui-v3
npm run build -q 2>/dev/null
if [ $? -ne 0 ]; then
    echo "✗ 前端构建失败"
    exit 1
fi
cd ..

# 清理旧文件再复制
docker exec junsong-nginx rm -rf /home/junsong/projects/junsong-ui/ 2>/dev/null || true
docker cp junsong-ui-v3/dist/. junsong-nginx:/home/junsong/projects/junsong-ui/
docker restart junsong-nginx
echo "✓ 前端已更新"

echo ""
echo "=========================================="
echo "  经营决策台部署完成！"
echo "  $(date '+%Y-%m-%d %H:%M:%S')"
echo "=========================================="
echo ""
echo "更新范围："
echo "  ✓ 菜单 SQL (3 个文件)"
echo "  ✓ 财务模块 (junsong-modules-finance)"
echo "  ✓ 会员模块 (junsong-modules-member)"
echo "  ✓ 前端 (junsong-ui-v3)"
echo ""
echo "查看日志："
echo "  docker logs -f junsong-modules-finance"
echo "  docker logs -f junsong-modules-member"
echo "  docker logs -f junsong-nginx"
