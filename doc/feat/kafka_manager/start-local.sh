#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose-zk.yml"

echo "=== Starting 3-node ZK ensemble on localhost ==="

docker compose -f "$COMPOSE_FILE" down -v 2>/dev/null || true
docker compose -f "$COMPOSE_FILE" up -d

echo ""
echo "=== Waiting for ZK ensemble (20s) ==="
sleep 20

echo ""
echo "=== ZK sanity check ==="
for port in 2181 2182 2183; do
    if echo "ruok" | timeout 3 nc -w 2 localhost $port > /dev/null 2>&1; then
        echo "  port $port: OK"
    else
        echo "  port $port: FAILED"
    fi
done

echo ""
echo "Done. ZK: localhost:2181,2182,2183"
echo "Connection string: localhost:2181,localhost:2182,localhost:2183"
