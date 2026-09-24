#!/usr/bin/env bash
# Usage: expect-count.sh <topic> <expected> — waits up to 60s for the topic to hold <expected> records
set -euo pipefail
for _ in $(seq 30); do
  count=$(docker compose exec -T kafka kafka-get-offsets --bootstrap-server kafka:29092 --topic "$1" \
    | awk -F: '{s += $3} END {print s + 0}')
  echo "$1: $count / $2"
  [ "$count" -ge "$2" ] && exit 0
  sleep 2
done
exit 1
