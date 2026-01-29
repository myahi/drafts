#!/usr/bin/env bash
#export JASYPT_ENCRYPTOR_PASSWORD='Eai_Dev_C@mel'
set -euo pipefail

usage() {
  cat <<EOF
Usage:
  appctl.sh start   --jar <jar> --props <application.properties> --logback <logback.xml> --profile <profile> [--name <appname>] [--run <runDir>]
  appctl.sh stop    --jar <jar> [--name <appname>] [--run <runDir>]
  appctl.sh restart --jar <jar> --props <application.properties> --logback <logback.xml> --profile <profile> [--name <appname>] [--run <runDir>]
  appctl.sh status  --jar <jar> [--name <appname>] [--run <runDir>]
EOF
}

# Defaults
NAME=""
RUN_DIR=""
JAR=""
PROPS=""
LOGBACK=""
PROFILE=""

CMD="${1:-}"
shift || true

# Parse args
while [[ $# -gt 0 ]]; do
  case "$1" in
    --name)    NAME="${2:-}"; shift 2 ;;
    --run)     RUN_DIR="${2:-}"; shift 2 ;;
    --jar)     JAR="${2:-}"; shift 2 ;;
    --props)   PROPS="${2:-}"; shift 2 ;;
    --logback) LOGBACK="${2:-}"; shift 2 ;;
    --profile) PROFILE="${2:-}"; shift 2 ;;
    -h|--help) usage; exit 0 ;;
    *) echo "Unknown arg: $1"; usage; exit 2 ;;
  esac
done

if [[ -z "$CMD" ]]; then usage; exit 2; fi
if [[ -z "$JAR" ]]; then echo "Missing --jar"; usage; exit 2; fi

# Derive name/run dir
if [[ -z "$NAME" ]]; then
  base="$(basename "$JAR")"
  NAME="${base%.jar}"
fi
if [[ -z "$RUN_DIR" ]]; then
  RUN_DIR="$(cd "$(dirname "$JAR")/.." 2>/dev/null && pwd)/run" || RUN_DIR="/tmp"
fi

PID_FILE="$RUN_DIR/$NAME.pid"
mkdir -p "$RUN_DIR"

is_running() {
  [[ -f "$PID_FILE" ]] && kill -0 "$(cat "$PID_FILE")" 2>/dev/null
}

do_start() {
  if [[ -z "$PROPS" || -z "$LOGBACK" || -z "$PROFILE" ]]; then
    echo "Missing --props/--logback/--profile for start"
    usage; exit 2
  fi

  [[ -f "$JAR" ]]     || { echo "Jar not found: $JAR"; exit 1; }
  [[ -f "$PROPS" ]]   || { echo "Props not found: $PROPS"; exit 1; }
  [[ -f "$LOGBACK" ]] || { echo "Logback not found: $LOGBACK"; exit 1; }

  if is_running; then
    echo "[$NAME] Already running (PID $(cat "$PID_FILE"))"
    exit 1
  fi

  echo "[$NAME] Starting (profile=$PROFILE)..."

  nohup java \
    -Dspring.profiles.active="$PROFILE" \
    -Dspring.config.location="$PROPS" \
    -Dlogging.config="$LOGBACK" \
    -jar "$JAR" \
    > /dev/null 2>&1 &

  echo $! > "$PID_FILE"
  echo "[$NAME] Started (PID $(cat "$PID_FILE"))"
}

do_stop() {
  if [[ ! -f "$PID_FILE" ]]; then
    echo "[$NAME] No PID file ($PID_FILE). Not running?"
  fi


  PID="$(cat "$PID_FILE")"
  if ! kill -0 "$PID" 2>/dev/null; then
    echo "[$NAME] PID $PID not running. Cleaning PID file."
    rm -f "$PID_FILE"
    exit 0
  fi

  echo "[$NAME] Stopping (PID $PID)..."
  kill "$PID" || true

  for _ in {1..20}; do
    if kill -0 "$PID" 2>/dev/null; then
      sleep 1
    else
      rm -f "$PID_FILE"
      echo "[$NAME] Stopped"
      exit 0
    fi
  done

  echo "[$NAME] Force kill (PID $PID)"
  kill -9 "$PID" || true
  rm -f "$PID_FILE"
  echo "[$NAME] Stopped (forced)"
}

do_status() {
  if is_running; then
    echo "[$NAME] RUNNING (PID $(cat "$PID_FILE"))"
    exit 0
  fi
  echo "[$NAME] STOPPED"
  exit 3
}

case "$CMD" in
  start)   do_start ;;
  stop)    do_stop ;;
  restart) do_stop ; sleep 2; do_start ;;
  status)  do_status ;;
  *) echo "Unknown command: $CMD"; usage; exit 2 ;;
esac
