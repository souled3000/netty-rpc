#!/bin/sh

PROCESS_NAME="GeLiSrv"

ps -ef|grep $PROCESS_NAME | grep java | grep -v grep | awk '{print $2}' |while read pid
do
        kill ${pid}
        declare msg="进程名称:${PROCESS_NAME},PID:${pid}于`date` 成功停止"
        echo $msg
        echo $msg >>../logs/logout.log
        exit 1
done && echo "服务未启动，无需停止"
