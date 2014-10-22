#!/bin/sh

# 目的:设置通用环境变量

# 作者:李春江
# 创建时间:2012-10-10

# *************************************************************************
# JAVA_OPTIONS - java启动选项
# JAVA_VM      - jvm选项
# MEM_ARGS     - 内存参数
# *************************************************************************

echo "*************************************************"

echo "初始化通用环境参数"
JAVA_HOME="/root/jdk1.7.0_51"
echo "JAVA_HOME=${JAVA_HOME}"

APP_HOME="/root/netty-rpc"
export APP_HOME
echo "APP_HOME=${APP_HOME}"

LIB_HOME="${APP_HOME}/lib"
export LIB_HOME
echo "LIB_HOME=${LIB_HOME}"

COMMON_CONFIG_HOME="${APP_HOME}/config"
echo "COMMON_CONFIG_HOME=${COMMON_CONFIG_HOME}"

#UNIX环境连接主lib目录下的每一个jar文件，windows环境请修改
CP=
for file in ${LIB_HOME}/*;
do CP=${CP}:$file;
done
for file in ${APP_HOME}/*.jar
do CP=${CP}:$file;
done

CLASSPATH=".:${COMMON_CONFIG_HOME}${CP}"
export CLASSPATH

JAVA_OPTIONS="  "

MEM_ARGS="-Xms128m -Xmx1024m"

echo "初始化通用环境参数完成"

#echo $CLASSPATH

echo "*************************************************"

