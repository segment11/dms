package script.tpl

def nodeIp = super.binding.getProperty('nodeIpList') as List<String>
def vip = super.binding.getProperty('vip') as List<String>

def mysqlUser = super.binding.getProperty('mysqlUser') as String
def mysqlPassword = super.binding.getProperty('mysqlPassword') as String
def mysqlPort = super.binding.getProperty('mysqlPort') as int

List<String> list = []

list << '''
#!/bin/bash

isitdead=$1
cluster=$2
oldmaster=$3
newmaster=$4
'''

list << """
mysqluser="${mysqlUser}"
mysqlPort="${mysqlPort}"
export MYSQL_PWD="${mysqlPassword}"

array=(ens32 "${vip}" root "${nodeIp}")
""".toString()

list << '''

logfile="/var/log/orch_hook.log"

if [[ $isitdead == "DeadMaster" ]]; then

	interface=${array[0]}
	IP=${array[1]}
	user=${array[2]}

	if [ ! -z ${IP} ]; then

		echo $(date)
		echo "Revocering from: $isitdead"
		echo "New master is: $newmaster"
		echo "/opt/orchestrator/orch_vip.sh -d 1 -n $newmaster -i ${interface} -I ${IP} -u ${user} -o $oldmaster" | tee $logfile
		/opt/orchestrator/orch_vip.sh -d 1 -n $newmaster -i ${interface} -I ${IP} -u ${user} -o $oldmaster
	else

		echo "Cluster does not exist!" | tee $logfile

	fi
elif [[ $isitdead == "DeadIntermediateMasterWithSingleSlaveFailingToConnect" ]]; then

	interface=${array[0]}
	IP=${array[3]}
	user=${array[2]}
	slavehost=$(echo $5 | cut -d":" -f1)

	echo $(date)
	echo "Revocering from: $isitdead"
	echo "New intermediate master is: $slavehost"
	echo "/opt/orchestrator/orch_vip.sh -d 1 -n $slavehost -i ${interface} -I ${IP} -u ${user} -o $oldmaster" | tee $logfile
	/opt/orchestrator/orch_vip.sh -d 1 -n $slavehost -i ${interface} -I ${IP} -u ${user} -o $oldmaster

elif
	[[ $isitdead == "DeadIntermediateMaster" ]]
then

	interface=${array[0]}
	IP=${array[3]}
	user=${array[2]}
	slavehost=$(echo $5 | sed -E "s/:[0-9]+//g" | sed -E "s/,/ /g")
	showslave=$(mysql -h$newmaster -u$MYSQL_USER -P $mysqlPort -sN -e "SHOW SLAVE HOSTS;" | awk '{print $2}')
	newintermediatemaster=$(echo $slavehost $showslave | tr ' ' '\n' | sort | uniq -d)

	echo $(date)
	echo "Revocering from: $isitdead"
	echo "New intermediate master is: $newintermediatemaster"
	echo "/opt/orchestrator/orch_vip.sh -d 1 -n $newintermediatemaster -i ${interface} -I ${IP} -u ${user} -o $oldmaster" | tee $logfile
	/opt/orchestrator/orch_vip.sh -d 1 -n $newintermediatemaster -i ${interface} -I ${IP} -u ${user} -o $oldmaster

fi
'''

list.join("\r\n")