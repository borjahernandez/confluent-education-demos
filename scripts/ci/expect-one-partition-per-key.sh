#!/usr/bin/env bash
# Usage: expect-one-partition-per-key.sh <topic> — fails if any key appears in more than one partition
set -euo pipefail
docker compose exec -T kafka kafka-console-consumer --bootstrap-server kafka:29092 --topic "$1" \
  --from-beginning --timeout-ms 10000 --formatter-property print.key=true --formatter-property print.partition=true \
  --formatter-property key.separator='|' > records.txt || true
# lines look like: Partition:3|United States|<value>
awk -F'|' '{print $2 "|" $1}' records.txt | sort -u | cut -d'|' -f1 | uniq -d > split-keys.txt
if [ -s split-keys.txt ]; then echo "keys spread over several partitions:"; cat split-keys.txt; exit 1; fi
echo "every key lives in exactly one partition ($(cut -d'|' -f2 records.txt | sort -u | wc -l) keys)"
