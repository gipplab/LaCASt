#!/bin/bash

PATH_MATHOID_STARTUP="/home/andreg-p/Projects/MathoidMinimal/start.sh"
PATH_ELASTICSEARCH_STARTUP="/opt/elasticsearch/bin/elasticsearch"

while getopts m:e: flag
do
  case "${flag}" in
    m) PATH_MATHOID_STARTUP=1;;
    e) PATH_ELASTICSEARCH_STARTUP=1;;
    *) printf "Unknown flag. Only support:\n\t-m\t Mathoid startup script path\n\t-e\t Elasticsearch startup path\n"
      exit 1
  esac
done

function on_exit() {
  echo "" # empty line
  echo "Received shutdown signal. Stop elasticsearch and mathoid"
  for pid in "${pids_memory[@]}"
  do
    echo "[$pid] Receive kill request"
    kill "$pid"
    wait "$pid"
    echo "[$pid] Done. Process is down"
  done
  echo "Shutdown all sub-processes"
  exit 0
}

# memory of process IDs to kill mathoid/elasticsearch later
pids_memory=()

[[ $PATH_MATHOID_STARTUP =~ (.*)/(.*?) ]]
MATHOID_PATH=${BASH_REMATCH[1]};
MATHOID_SCRIPT=${BASH_REMATCH[2]};

echo "Start Mathoid"
cd "$MATHOID_PATH" || exit 1
sh "$MATHOID_SCRIPT" & pids_memory+=("$!")

printf "[%s] Mathoid running\n" "${pids_memory[-1]}"

echo "Start Elasticsearch"
exec "$PATH_ELASTICSEARCH_STARTUP" & pids_memory+=("$!")

printf "[%s] Elasticsearch running\n" "${pids_memory[-1]}"

trap on_exit SIGINT SIGTERM
sleep infinity