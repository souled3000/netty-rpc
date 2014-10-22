#!/bin/sh

sh monitor_geli.sh >a
read PROCESS_ALIVE_STATUS<a
rm a
if [ "$PROCESS_ALIVE_STATUS" = "PROCESS_EXIST" ];
then
		echo "此进程已经启动了,不能重复启动"
        exit 0;
fi

. "./setEnv.sh"

CLASSPATH="${CLASSPATH}"
export CLASSPATH

echo "CLASSPATH=${CLASSPATH}"

MEM_ARGS="-Xms512m -Xmx1024m"

echo "MEM_ARGS=${MEM_ARGS}"

echo "JAVA_OPTIONS=${JAVA_OPTIONS}"

${JAVA_HOME}/bin/java ${MEM_ARGS} ${JAVA_OPTIONS} com.blackcrystalinfo.platform.powersocket.GeLiSrv 2>&1 >>${APP_HOME}/logs/all.log &
echo "启动完成,请查看日志"
