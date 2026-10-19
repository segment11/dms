#!/bin/bash
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="$SCRIPT_DIR/docker-compose-zk.yml"

echo "=== Stopping ZK cluster ==="
docker compose -f "$COMPOSE_FILE" down -v 2>/dev/null || true
echo "Done."
