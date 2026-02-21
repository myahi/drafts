#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/set_classpath.sh"

usage() {
  echo "Usage:"
  echo "  $0 --env DEVEAI --batch BATCH_NAME --scenarios 'MAESTRO/...'"
  exit 2
}

ENV_NAME="DEVEAI"
BATCH=""
SCENARIOS=""
VM_PARAMS=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --env) ENV_NAME="$2"; shift 2;;
    --batch) BATCH="$2"; shift 2;;
    --scenarios) SCENARIOS="$2"; shift 2;;
    --vmparams) VM_PARAMS="$2"; shift 2;;
    *) echo "Unknown arg: $1"; usage;;
  esac
done

[[ -n "${BATCH}" && -n "${SCENARIOS}" ]] || usage

# Windows -> Linux path
SCENARIOS="${SCENARIOS//\\//}"

LOG_DIR="${EASYTNR_HOME}/logs/${ENV_NAME}/${BATCH}/$(date +%Y%m%d_%H%M%S)"
mkdir -p "${LOG_DIR}"
LOG_FILE="${LOG_DIR}/run.log"

echo "=== EASYTNR BATCH START ===" | tee -a "${LOG_FILE}"
echo "ENV=${ENV_NAME}" | tee -a "${LOG_FILE}"
echo "BATCH=${BATCH}" | tee -a "${LOG_FILE}"
echo "SCENARIOS=${SCENARIOS}" | tee -a "${LOG_FILE}"

# ⚠️ A REMPLACER par ton vrai main class
MAIN_CLASS="TO_DEFINE"

JAVA_ARGS="${VM_PARAMS} \
-DENV=${ENV_NAME} \
-DBATCH_NAME=${BATCH} \
-DSCENARIOS=${SCENARIOS} \
-DTNRFolder=${EASYTNR_HOME} \
-Dtnr.logs.dir=${LOG_DIR}"

set +e
java ${JAVA_ARGS} -cp "${EASYTNR_CP}" "${MAIN_CLASS}" \
  2>&1 | tee -a "${LOG_FILE}"
RC=${PIPESTATUS[0]}
set -e

echo "ExitCode=${RC}" | tee -a "${LOG_FILE}"
exit ${RC}
