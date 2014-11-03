#!/bin/sh
. "properties"
BASE_APP_HOME="${APP_HOME}"
sh $BASE_APP_HOME/bin/monitor_process.sh ${clz}
