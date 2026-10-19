#!/bin/bash
# E2E test runner for Kafka Manager
# Usage:
#   ./run-e2e.sh              run all tests
#   ./run-e2e.sh 1.1          run only test 1.1
#   ./run-e2e.sh 2            run group 2 only

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
INFRA_DIR="$SCRIPT_DIR"
DMS_DIR="/home/kerry/ws/dms/dms"

BASE_URL="http://localhost:5010/dms"
ZK_CONNECT="localhost:2181,localhost:2182,localhost:2183"

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

info()  { echo -e "${GREEN}[INFO]${NC} $1"; }
warn()  { echo -e "${YELLOW}[WARN]${NC} $1"; }
fail()  { echo -e "${RED}[FAIL]${NC} $1"; }
phase() { echo -e "${CYAN}==>${NC} $1"; }

# --- HTTP helpers ---
# All calls use cookie-based auth (Auth-Token from login)

COOKIE_FILE="/tmp/dms_e2e_cookies.txt"

http_get() {
    local url="$1"; shift
    curl -s -b "$COOKIE_FILE" "$@" "$url"
}

http_post() {
    local url="$1"; shift
    curl -s -b "$COOKIE_FILE" -c "$COOKIE_FILE" \
        -H "Content-Type: application/json" "$@" "$url"
}

# --- Login ---
login() {
    phase "Logging in as admin/123456..."
    local resp=$(curl -s -c "$COOKIE_FILE" -X POST \
        "$BASE_URL/login" \
        -d "user=admin&password=123456" \
        -w "\n%{http_code}" -o /dev/null)
    local http_code=$(echo "$resp" | tail -1)
    if [ "$http_code" = "302" ]; then
        info "Login successful (302 redirect)"
    else
        fail "Login failed with HTTP $http_code"
        return 1
    fi
}

# --- Wait helpers ---
wait_service_status() {
    local sid=$1 expected=$2 max_wait=${3:-120}
    local count=0
    while [ $count -lt $max_wait ]; do
        local resp status
        resp=$(http_get "$BASE_URL/kafka/service/one?id=$sid")
        status=$(echo "$resp" | grep -o '"status":"[^"]*"' | head -1 | sed 's/.*://' | tr -d '"')
        if [ "$status" = "$expected" ]; then
            return 0
        fi
        echo "  status=$status (waited ${count}s)"
        sleep 5; count=$((count + 5))
    done
    return 1
}

# --- Test runner ---
T=0 P=0 F=0

run() {
    local num=$1 name=$2 cmd=$3
    T=$((T+1))
    echo ""
    echo -e "${CYAN}--- Test $num: $name ---${NC}"
    echo "CMD: $cmd"

    local t0=$(date +%s)
    local out rc=0
    set +e
    out=$(eval "$cmd" 2>&1); rc=$?
    set -e
    local dt=$(($(date +%s) - t0))

    # Check: exit 0, no error keyword, has id or expected field
    local err=$(echo "$out" | grep -iE '"(error|exception|message)":"[^"]*error|error:|exception' | head -1 || true)
    local has_id=$(echo "$out" | grep -o '"id":[0-9]*' | head -1 || true)
    local has_flag=$(echo "$out" | grep -o '"flag":true' | head -1 || true)

    if [ $rc -eq 0 ] && [ -n "$has_id" ] && [ -z "$err" ]; then
        echo -e "${GREEN}PASS${NC} (${dt}s) id=$has_id"
        echo "Response: $(echo $out | head -c 400)"
        P=$((P+1))
    elif [ $rc -eq 0 ] && [ -n "$has_flag" ] && [ -z "$err" ]; then
        echo -e "${GREEN}PASS${NC} (${dt}s) flag=true"
        echo "Response: $(echo $out | head -c 400)"
        P=$((P+1))
    elif [ $rc -eq 0 ] && [ -z "$err" ] && [ -n "$out" ]; then
        echo -e "${GREEN}PASS${NC} (${dt}s)"
        echo "Response: $(echo $out | head -c 400)"
        P=$((P+1))
    else
        echo -e "${RED}FAIL${NC} (${dt}s) rc=$rc"
        [ -n "$err" ] && echo "Error: $(echo $err | head -c 200)"
        [ -n "$has_id" ] && echo "Got id: $has_id"
        echo "Response: $(echo $out | head -c 500)"
        F=$((F+1))
    fi
}

# --- Main ---
TEST_FILTER=${1:-}

check_docker() { docker info > /dev/null 2>&1 && info "Docker OK" || { fail "Docker unavailable"; exit 1; }; }
check_zk() {
    if ! echo "ruok" | timeout 2 nc -w 1 localhost 2181 > /dev/null 2>&1; then
        warn "ZK not running on 2181. Starting..."
        bash "$INFRA_DIR/start-local.sh"
    else
        info "ZK ensemble already running"
    fi
}

echo "========================================"
echo "Kafka Manager E2E Test Runner"
echo "========================================"
echo "ZK: $ZK_CONNECT"
echo "API base: $BASE_URL"
echo "Filter: ${TEST_FILTER:-all}"
echo ""

check_docker
check_zk
login

echo ""
echo "========================================"
echo "Running Tests"
echo "========================================"

# =============================================================
# Group 1: Standalone Service
# =============================================================
if [ -z "$TEST_FILTER" ] || [[ "1" == "$TEST_FILTER" ]] || \
   [[ " 1.1 1.2 1.3 1.4 1.5 1.6 1.7 " =~ " $TEST_FILTER " ]]; then

    echo ""
    phase ">>> Group 1: Standalone Service"

    SOLO_NAME="e2e_solo_$(date +%s)"
    SOLO_ID=""

    run "1.1" "Create standalone service" \
        "http_post '$BASE_URL/kafka/service/add' -d '{\"name\":\"$SOLO_NAME\",\"mode\":\"standalone\",\"brokers\":1,\"zkConnectString\":\"$ZK_CONNECT\",\"kafkaVersion\":\"2.8.2\"}'"

    sleep 5
    SOLO_ID=$(http_get "$BASE_URL/kafka/service/list?name=$SOLO_NAME" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
    [ -n "$SOLO_ID" ] && info "Standalone ID: $SOLO_ID" || warn "Standalone ID not found"

    if [ -n "$SOLO_ID" ]; then
        info "Waiting for standalone to be running (max 120s)..."
        wait_service_status "$SOLO_ID" "running" 120 && info "Standalone is running" || warn "Standalone may not be running"

        run "1.2" "Create topic" \
            "http_post '$BASE_URL/kafka/topic/add' -d '{\"serviceId\":$SOLO_ID,\"name\":\"test-topic\",\"partitions\":4,\"replicationFactor\":1}'"

        sleep 2

        run "1.3" "List topics" \
            "http_get '$BASE_URL/kafka/topic/list?serviceId=$SOLO_ID'"

        run "1.4" "Get topic detail" \
            "http_get '$BASE_URL/kafka/topic/one?serviceId=$SOLO_ID&name=test-topic'"

        run "1.5" "Delete topic" \
            "http_post '$BASE_URL/kafka/topic/delete' -d '{\"serviceId\":$SOLO_ID,\"name\":\"test-topic\"}'"
    fi

    [ -n "$SOLO_ID" ] && echo "$SOLO_ID" > /tmp/solo_id
fi

# =============================================================
# Group 2: Cluster Service
# =============================================================
if [ -z "$TEST_FILTER" ] || [[ "2" == "$TEST_FILTER" ]] || \
   [[ " 2.1 2.2 2.3 2.4 2.5 2.6 " =~ " $TEST_FILTER " ]]; then

    echo ""
    phase ">>> Group 2: Cluster Service"

    CLS_NAME="e2e_cls_$(date +%s)"
    CLS_ID=""

    run "2.1" "Create 3-broker cluster" \
        "http_post '$BASE_URL/kafka/service/add' -d '{\"name\":\"$CLS_NAME\",\"mode\":\"cluster\",\"brokers\":3,\"zkConnectString\":\"$ZK_CONNECT\",\"kafkaVersion\":\"2.8.2\"}'"

    sleep 5
    CLS_ID=$(http_get "$BASE_URL/kafka/service/list?name=$CLS_NAME" | grep -o '"id":[0-9]*' | head -1 | grep -o '[0-9]*')
    [ -n "$CLS_ID" ] && info "Cluster ID: $CLS_ID" || warn "Cluster ID not found"

    if [ -n "$CLS_ID" ]; then
        info "Waiting for cluster to be running (max 180s)..."
        wait_service_status "$CLS_ID" "running" 180 && info "Cluster is running" || {
            warn "Cluster may not be running. Last status check:"
            http_get "$BASE_URL/kafka/service/one?id=$CLS_ID" | head -c 300
            echo ""
            info "Checking job list:"
            http_get "$BASE_URL/kafka/job/list?serviceId=$CLS_ID" | head -c 500
        }

        run "2.2" "Create replicated topic" \
            "http_post '$BASE_URL/kafka/topic/add' -d '{\"serviceId\":$CLS_ID,\"name\":\"repl-topic\",\"partitions\":8,\"replicationFactor\":3}'"

        sleep 2

        run "2.4" "Get service detail" \
            "http_get '$BASE_URL/kafka/service/one?id=$CLS_ID'"

        run "2.5" "List running clusters" \
            "http_get '$BASE_URL/kafka/service/list?mode=cluster&status=running'"
    fi

    [ -n "$CLS_ID" ] && echo "$CLS_ID" > /tmp/cls_id
fi

# =============================================================
# Group 4: Preferred Replica Election
# =============================================================
if [ -z "$TEST_FILTER" ] || [[ "4" == "$TEST_FILTER" ]] || \
   [[ " 4.3 4.4 " =~ " $TEST_FILTER " ]]; then

    echo ""
    phase ">>> Group 4: Preferred Replica Election"

    if [ -f /tmp/cls_id ]; then
        CID=$(cat /tmp/cls_id)
        run "4.3a" "Preferred replica election" \
            "http_post '$BASE_URL/kafka/service/preferred-replica-election' -d '{\"id\":$CID}'"
        sleep 3
        run "4.3b" "Election again (stale path should be cleaned)" \
            "http_post '$BASE_URL/kafka/service/preferred-replica-election' -d '{\"id\":$CID}'"
    else
        warn "Skipping: no cluster"
    fi
fi

# =============================================================
# Group 9: Consumer Group Inspection
# =============================================================
if [ -z "$TEST_FILTER" ] || [[ "9" == "$TEST_FILTER" ]] || \
   [[ " 9.1 9.2 9.3 9.4 9.5 " =~ " $TEST_FILTER " ]]; then

    echo ""
    phase ">>> Group 9: Consumer Group Inspection"

    if [ -f /tmp/cls_id ]; then
        CID=$(cat /tmp/cls_id)

        run "9.1" "List consumer groups" \
            "http_get '$BASE_URL/kafka/consumer/list?serviceId=$CID'"

        run "9.2" "Consumer group detail (non-existent)" \
            "http_get '$BASE_URL/kafka/consumer/one?serviceId=$CID&groupId=nonexistent-group'"

        run "9.3" "Consumer lag (non-existent group)" \
            "http_get '$BASE_URL/kafka/consumer/lag?serviceId=$CID&groupId=nonexistent-group'"

        run "9.4" "Consumer lag (empty group)" \
            "http_get '$BASE_URL/kafka/consumer/lag?serviceId=$CID&groupId='"
    else
        warn "Skipping: no cluster"
    fi
fi

# =============================================================
# Group 10: Job Observability
# =============================================================
if [ -z "$TEST_FILTER" ] || [[ "10" == "$TEST_FILTER" ]] || \
   [[ " 10.1 10.2 10.3 " =~ " $TEST_FILTER " ]]; then

    echo ""
    phase ">>> Group 10: Job Observability"

    if [ -f /tmp/cls_id ]; then
        CID=$(cat /tmp/cls_id)
        run "10.1" "Job history" \
            "http_get '$BASE_URL/kafka/job/list?serviceId=$CID'"
    fi
fi

# =============================================================
# Group 11: Additional Endpoints
# =============================================================
if [ -z "$TEST_FILTER" ] || [[ "11" == "$TEST_FILTER" ]] || \
   [[ " 11.1 11.2 11.3 11.4 11.5 11.6 11.7 11.8 " =~ " $TEST_FILTER " ]]; then

    echo ""
    phase ">>> Group 11: Additional Endpoints"

    if [ -f /tmp/cls_id ]; then
        CID=$(cat /tmp/cls_id)
        run "11.3" "Update broker config" \
            "http_post '$BASE_URL/kafka/service/update-config' -d '{\"id\":$CID,\"configOverrides\":{\"num.io.threads\":\"8\"}}'"
        sleep 2
        run "11.4" "Update config merge" \
            "http_post '$BASE_URL/kafka/service/update-config' -d '{\"id\":$CID,\"configOverrides\":{\"num.network.threads\":\"8\"}}'"
    fi

    if [ -f /tmp/solo_id ]; then
        SID=$(cat /tmp/solo_id)
        run "11.1" "Stop standalone" \
            "http_post '$BASE_URL/kafka/service/stop' -d '{\"id\":$SID}'"
    fi
fi

# =============================================================
# Summary
# =============================================================
echo ""
echo "========================================"
echo "Results: ${GREEN}$P passed${NC}, ${RED}$F failed${NC}, $T total"
echo "========================================"

[ $F -gt 0 ] && exit 1 || info "All tests passed"
