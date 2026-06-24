#!/bin/sh
# =============================================
# JunSong Cloud Docker 启动脚本
# 用于管理容器的启动和停止
# =============================================
# 更新记录：
# 2026-06-02: 添加member、finance业务模块支持
#             修复服务注册到Nacos问题
# =============================================

# 使用说明，用来提示输入参数
usage() {
	echo "=========================================="
	echo "  JunSong Cloud Docker 启动脚本"
	echo "  Version: 2026-06-02"
	echo "=========================================="
	echo "Usage: sh deploy.sh [port|base|modules|member|finance|all|stop|rm|status|logs]"
	echo ""
	echo "参数说明:"
	echo "  port    - 开启所需端口（适用于Linux系统）"
	echo "  base    - 启动基础环境（MySQL、Redis、Nacos）"
	echo "  modules - 启动核心业务模块"
	echo "  member  - 启动会员模块"
	echo "  finance - 启动财务模块"
	echo "  all     - 启动所有服务"
	echo "  stop    - 停止所有服务"
	echo "  rm      - 删除所有服务容器"
	echo "  status  - 查看服务状态"
	echo "  logs    - 查看服务日志"
	echo ""
	exit 1
}

# 开启所需端口
port(){
	echo "开启所需端口..."
	firewall-cmd --add-port=80/tcp --permanent
	firewall-cmd --add-port=8080/tcp --permanent
	firewall-cmd --add-port=8848/tcp --permanent
	firewall-cmd --add-port=9848/tcp --permanent
	firewall-cmd --add-port=9849/tcp --permanent
	firewall-cmd --add-port=6379/tcp --permanent
	firewall-cmd --add-port=3306/tcp --permanent
	firewall-cmd --add-port=9100/tcp --permanent
	firewall-cmd --add-port=9200/tcp --permanent
	firewall-cmd --add-port=9201/tcp --permanent
	firewall-cmd --add-port=9202/tcp --permanent
	firewall-cmd --add-port=9203/tcp --permanent
	firewall-cmd --add-port=9204/tcp --permanent
	firewall-cmd --add-port=9205/tcp --permanent
	firewall-cmd --add-port=9300/tcp --permanent
	service firewalld restart
	echo "端口开启完成"
}

# 启动基础环境（必须）
base(){
	echo "启动基础环境（MySQL、Redis、Nacos）..."
	docker-compose up -d junsong-mysql junsong-redis junsong-nacos
	echo "基础环境启动中，请等待30秒后再启动业务模块"
}

# 启动程序模块（必须）
modules(){
	echo "启动核心业务模块..."
	docker-compose up -d junsong-nginx junsong-gateway junsong-auth junsong-modules-system junsong-modules-gen junsong-modules-job junsong-modules-file
	echo "核心业务模块启动完成"
}

# 启动会员模块
member(){
	echo "启动会员模块..."
	docker-compose up -d junsong-modules-member
	echo "会员模块启动完成"
}

# 启动财务模块
finance(){
	echo "启动财务模块..."
	docker-compose up -d junsong-modules-finance
	echo "财务模块启动完成"
}

# 启动所有服务
all(){
	echo "启动所有服务..."
	echo "1. 启动基础环境..."
	docker-compose up -d junsong-mysql junsong-redis junsong-nacos
	sleep 30
	echo "2. 启动业务模块..."
	docker-compose up -d junsong-nginx junsong-gateway junsong-auth junsong-modules-system junsong-modules-gen junsong-modules-job junsong-modules-file junsong-modules-member junsong-modules-finance
	echo "所有服务启动完成"
}

# 关闭所有环境/模块
stop(){
	echo "停止所有服务..."
	docker-compose stop
	echo "所有服务已停止"
}

# 删除所有环境/模块
rm(){
	echo "删除所有服务容器..."
	docker-compose rm -f
	echo "所有服务容器已删除"
}

# 查看服务状态
status(){
	echo "=========================================="
	echo "  服务状态"
	echo "=========================================="
	docker-compose ps
	echo ""
	echo "访问地址："
	echo "  前端：http://localhost"
	echo "  网关：http://localhost:8080"
	echo "  Nacos：http://localhost:8848/nacos"
}

# 查看服务日志
logs(){
	echo "=========================================="
	echo "  服务日志（最近10行）"
	echo "=========================================="
	docker-compose logs --tail=10
}

# 根据输入参数，选择执行对应方法，不输入则执行使用说明
case "$1" in
"port")
	port
;;
"base")
	base
;;
"modules")
	modules
;;
"member")
	member
;;
"finance")
	finance
;;
"all")
	all
;;
"stop")
	stop
;;
"rm")
	rm
;;
"status")
	status
;;
"logs")
	logs
;;
*)
	usage
;;
esac
