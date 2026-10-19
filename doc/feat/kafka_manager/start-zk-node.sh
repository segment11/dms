#!/bin/bash
set -e

ZK_HOME="/tmp/apache-zookeeper-3.8.4-bin"
ZK_DATA_DIR="/tmp/zk"
ZK_CLIENT_PORT=${1:-2181}
ZK_ID=${2:-1}

mkdir -p "$ZK_DATA_DIR/data" "$ZK_DATA_DIR/logs"

# Create myid
echo "$ZK_ID" > "$ZK_DATA_DIR/data/myid"

# Create config
cat > "$ZK_DATA_DIR/zoo.cfg" << EOF
tickTime=2000
initLimit=5
syncLimit=2
dataDir=$ZK_DATA_DIR/data
dataLogDir=$ZK_DATA_DIR/logs
clientPort=$ZK_CLIENT_PORT
EOF

echo "Starting ZK on port $ZK_CLIENT_PORT with dataDir=$ZK_DATA_DIR"
"$ZK_HOME/bin/zkServer.sh" start-foreground "$ZK_DATA_DIR/zoo.cfg" &
sleep 2
echo "ZK $ZK_ID started on port $ZK_CLIENT_PORT"
