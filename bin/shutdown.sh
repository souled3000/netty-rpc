#!/bin/sh

# 作者:李春江
# 创建时间:2012-10-10
# 脚本目的:停止独立java进程
# 修改原因:
# 修改时间:
# 修改作者:

. "./properties"
ps -ef|grep ${clz} | grep java | grep -v grep | awk '{print $2}' |while read pid
do
        kill -9 ${pid}
        msg="进程名称:${clz},PID:${pid}于`date` 成功停止"
        echo $msg
        echo $msg >>../logs/logout.log
        exit 1
done && echo "服务未启动，无需停止"
