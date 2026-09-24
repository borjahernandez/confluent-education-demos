#!/usr/bin/env bash
# Usage: expect-partitions.sh <topic> <partitions>
set -euo pipefail
actual=$(docker compose exec -T kafka kafka-topics --bootstrap-server kafka:29092 --describe --topic "$1" \
  | grep -o 'PartitionCount: *[0-9]*' | grep -o '[0-9]*$')
echo "$1 has $actual partitions"
[ "$actual" -eq "$2" ]
