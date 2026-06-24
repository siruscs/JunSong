#!/bin/bash
# =============================================
# JunSong Cloud - 小程序编译脚本
# 用法: ./build-miniprogram.sh [dev|prod]
# =============================================

set -e

ENV=${1:-dev}
PROJECT_ROOT="/Users/sirius/Documents/TRAE/JunSong-Cloud"
MINIPROGRAM_DIR="${PROJECT_ROOT}/junsong-miniprogram"

echo "=========================================="
echo "  小程序编译 - ${ENV}环境"
echo "=========================================="

cd ${MINIPROGRAM_DIR}

# 检查依赖
echo ""
echo "[1/3] 检查依赖..."
if [ ! -d "node_modules" ]; then
    echo "安装小程序依赖..."
    npm install
fi
echo "✓ 依赖检查完成"

# 编译
echo ""
echo "[2/3] 编译小程序代码..."
if [ "${ENV}" = "prod" ]; then
    npm run build:mp-weixin
    OUTPUT_DIR="dist/build/mp-weixin"
else
    npm run dev:mp-weixin
    OUTPUT_DIR="dist/dev/mp-weixin"
fi

if [ $? -ne 0 ]; then
    echo "✗ 编译失败"
    exit 1
fi
echo "✓ 编译成功"

# 输出信息
echo ""
echo "[3/3] 编译完成"
echo ""
echo "=========================================="
echo "  编译完成！"
echo "=========================================="
echo ""
echo "输出目录: ${MINIPROGRAM_DIR}/${OUTPUT_DIR}"
echo ""
echo "使用微信开发者工具打开以下目录预览："
echo "  ${MINIPROGRAM_DIR}/${OUTPUT_DIR}"
echo ""
echo "或使用 HBuilderX 直接运行项目"
echo ""
