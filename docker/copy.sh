#!/bin/sh

# 复制项目的文件到对应docker路径，便于一键生成镜像。
usage() {
	echo "Usage: sh copy.sh"
	exit 1
}


# copy sql
echo "begin copy sql "
cp ../sql/ry_20260402.sql ./mysql/db
cp ../sql/ry_config_20260311.sql ./mysql/db

# copy html
echo "begin copy html "
cp -r ../junsong-ui/dist/** ./nginx/html/dist


# copy jar
echo "begin copy junsong-gateway "
cp ../junsong-gateway/target/junsong-gateway.jar ./junsong/gateway/jar

echo "begin copy junsong-auth "
cp ../junsong-auth/target/junsong-auth.jar ./junsong/auth/jar

echo "begin copy junsong-visual "
cp ../junsong-visual/junsong-monitor/target/junsong-visual-monitor.jar  ./junsong/visual/monitor/jar

echo "begin copy junsong-modules-system "
cp ../junsong-modules/junsong-system/target/junsong-modules-system.jar ./junsong/modules/system/jar

echo "begin copy junsong-modules-file "
cp ../junsong-modules/junsong-file/target/junsong-modules-file.jar ./junsong/modules/file/jar

echo "begin copy junsong-modules-job "
cp ../junsong-modules/junsong-job/target/junsong-modules-job.jar ./junsong/modules/job/jar

echo "begin copy junsong-modules-gen "
cp ../junsong-modules/junsong-gen/target/junsong-modules-gen.jar ./junsong/modules/gen/jar

