#!/bin/bash
# 通过 Nacos Open API 将 docker/nacos/conf 下的配置发布到控制台（不依赖 MySQL 导入）
set -e
cd "$(dirname "$0")/.."

NACOS_ADDR="${NACOS_ADDR:-http://127.0.0.1:8848}"
CONF_DIR="docker/nacos/conf"
GROUP="${NACOS_GROUP:-DEFAULT_GROUP}"
TENANT="${NACOS_TENANT:-}"

publish() {
  local data_id="$1"
  local type="${2:-yaml}"
  local file="${CONF_DIR}/${data_id}"
  if [[ ! -f "${file}" ]]; then
    echo "跳过（文件不存在）: ${data_id}"
    return
  fi
  echo "发布: ${data_id}"
  curl -s -X POST "${NACOS_ADDR}/nacos/v1/cs/configs" \
    --data-urlencode "dataId=${data_id}" \
    --data-urlencode "group=${GROUP}" \
    --data-urlencode "tenant=${TENANT}" \
    --data-urlencode "type=${type}" \
    --data-urlencode "content@${file}" | grep -q true && echo "  OK" || echo "  失败，请检查 Nacos 地址与版本"
}

echo "Nacos 地址: ${NACOS_ADDR}"
for f in application-dev.yml junsong-gateway-dev.yml junsong-auth-dev.yml junsong-system-dev.yml \
  junsong-finance-dev.yml junsong-member-dev.yml junsong-gen-dev.yml junsong-job-dev.yml \
  junsong-file-dev.yml junsong-monitor-dev.yml; do
  publish "${f}" yaml
done
publish "sentinel-junsong-gateway" json
echo "完成。请在控制台刷新配置列表（命名空间: public）。"
