#!/bin/bash
# =============================================
# JunSong Cloud - 小程序编译脚本
# 用法: ./build-miniprogram.sh [prod|dev]
# =============================================

set -e

ENV=${1:-prod}
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
case "${ENV}" in
    prod)
        if ! npm run build:mp-weixin; then
            echo "✗ 编译失败"
            exit 1
        fi
        OUTPUT_DIR="dist/build/mp-weixin"
        ;;
    dev)
        echo "开发模式会持续运行并监听文件变化，不会自动退出。"
        echo "按 Ctrl+C 停止开发服务器。"
        exec npm run dev:mp-weixin
        ;;
    *)
        echo "用法: $0 [prod|dev]"
        exit 2
        ;;
esac

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
