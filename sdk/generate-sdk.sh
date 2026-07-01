#!/bin/bash
set -e

# JunSong-Cloud 开放平台多语言SDK生成脚本
# 从open服务的OpenAPI文档自动生成Java/Python/Go/JavaScript SDK
#
# 用法: ./generate-sdk.sh
# 依赖: Node.js + npx

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
API_URL="${1:-http://localhost:9208/v3/api-docs}"
SDK_DIR="$SCRIPT_DIR/../sdk"

echo "=========================================="
echo "  JunSong-Cloud SDK 生成器"
echo "=========================================="
echo "API文档: $API_URL"
echo "输出目录: $SDK_DIR"
echo ""

mkdir -p "$SDK_DIR"

echo "[1/5] 下载OpenAPI文档..."
curl -s "$API_URL" -o "$SDK_DIR/openapi.json"
echo "  ✅ 文档已下载 ($(wc -c < "$SDK_DIR/openapi.json") bytes)"

echo "[2/5] 生成Java SDK..."
npx @openapitools/openapi-generator-cli generate \
  -i "$SDK_DIR/openapi.json" -g java -o "$SDK_DIR/sdk-java" \
  --additional-properties=groupId=com.junsong,artifactId=junsong-open-sdk,apiPackage=com.junsong.open.api,modelPackage=com.junsong.open.model,hideGenerationTimestamp=true
echo "  ✅ Java SDK生成完成"

echo "[3/5] 生成Python SDK..."
npx @openapitools/openapi-generator-cli generate \
  -i "$SDK_DIR/openapi.json" -g python -o "$SDK_DIR/sdk-python" \
  --additional-properties=packageName=junsong_open_sdk,projectName=junsong-open-sdk
echo "  ✅ Python SDK生成完成"

echo "[4/5] 生成Go SDK..."
npx @openapitools/openapi-generator-cli generate \
  -i "$SDK_DIR/openapi.json" -g go -o "$SDK_DIR/sdk-go" \
  --additional-properties=packageName=junsongsdk
echo "  ✅ Go SDK生成完成"

echo "[5/5] 生成JavaScript SDK..."
npx @openapitools/openapi-generator-cli generate \
  -i "$SDK_DIR/openapi.json" -g javascript -o "$SDK_DIR/sdk-js" \
  --additional-properties=projectName=junsong-open-sdk,moduleName=junsongOpenSdk
echo "  ✅ JavaScript SDK生成完成"

echo ""
echo "=========================================="
echo "  ✅ 全部SDK生成完成"
echo "=========================================="
echo "  Java:       sdk/sdk-java/"
echo "  Python:     sdk/sdk-python/"
echo "  Go:         sdk/sdk-go/"
echo "  JavaScript: sdk/sdk-js/"
echo "=========================================="
