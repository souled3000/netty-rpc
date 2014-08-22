#!/bin/sh

# 目的:启动对空服侧服务，用于接收来自空服的短信指令
# 作者:李春江
# 创建时间:2012-10-10
# 脚本目的:启动独立java进程
# 修改原因:
# 修改时间:
# 修改作者:

# *************************************************************************
# JAVA_OPTIONS - java启动选项
# JAVA_VM      - jvm选项
# MEM_ARGS     - 内存参数
# *************************************************************************

#判断进程是否重复启动
sh monitor_netty.sh >a
read PROCESS_ALIVE_STATUS<a
rm a
if [ "$PROCESS_ALIVE_STATUS" = "PROCESS_EXIST" ];
then
		echo "此进程已经启动了,不能重复启动"
        exit 0;
fi
#判断进程是否重复启动结束

. "./setEnv.sh"

CLASSPATH="${CLASSPATH}"
export CLASSPATH

echo "CLASSPATH=${CLASSPATH}"

MEM_ARGS="-Xms512m -Xmx1024m"

echo "MEM_ARGS=${MEM_ARGS}"

echo "JAVA_OPTIONS=${JAVA_OPTIONS}"

#启动命令行
#${JAVA_HOME}/bin/java ${MEM_ARGS} ${JAVA_OPTIONS} com.blackcrystalinfo.platform.powersocket.Main  2>&1 >>  ${APP_HOME}/bin/logs/all.log &
${JAVA_HOME}/bin/java ${MEM_ARGS} ${JAVA_OPTIONS} com.blackcrystalinfo.platform.powersocket.Main 2>&1 &
echo "启动完成,请查看日志"
