#!/usr/bin/env bash
set -euo pipefail

EASYTNR_HOME="/serveur_apps/easy-tnr"
LIB_DIR="${EASYTNR_HOME}/libs"
CONFIG_DIR="${EASYTNR_HOME}/config"
LOGS_DIR="${EASYTNR_HOME}/logs"

ENV_NAME="DEVEai"
SCENARIOS_SUBDIR=""

usage() {
  echo "Usage:"
  echo "  $0 --env DEVEai --scenarios 'MAESTRO/GLMX_TO_CALYPSO/REPO'"
  exit 2
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --env) ENV_NAME="$2"; shift 2;;
    --scenarios) SCENARIOS_SUBDIR="$2"; shift 2;;
    *) echo "Unknown arg: $1"; usage;;
  esac
done

[[ -n "${SCENARIOS_SUBDIR}" ]] || usage

CONF_FILE="${CONFIG_DIR}/${ENV_NAME}.tnrConfigFile.properties"
[[ -f "${CONF_FILE}" ]] || { echo "[ERROR] Conf introuvable: ${CONF_FILE}"; exit 2; }

# Windows "\" -> Linux "/"
SCENARIOS_SUBDIR="${SCENARIOS_SUBDIR//\\//}"

# Classpath
CP=""
for j in "${LIB_DIR}"/*.jar; do
  [[ -e "$j" ]] || continue
  CP="${CP}:$j"
done
CP="${CP}:${CONFIG_DIR}"
CP="${CP#:}"

TS="$(date +%Y%m%d_%H%M%S)"
RUN_DIR="${LOGS_DIR}/${ENV_NAME}/$(echo "${SCENARIOS_SUBDIR}" | tr '/' '_')/${TS}"
mkdir -p "${RUN_DIR}"
LOG_FILE="${RUN_DIR}/run.log"

echo "[INFO] ENV=${ENV_NAME}" | tee -a "${LOG_FILE}"
echo "[INFO] CONF=${CONF_FILE}" | tee -a "${LOG_FILE}"
echo "[INFO] SCENARIOS=${SCENARIOS_SUBDIR}" | tee -a "${LOG_FILE}"
echo "[INFO] LOG=${LOG_FILE}" | tee -a "${LOG_FILE}"

# ⚠️ à remplacer par le vrai main class
MAIN_CLASS="TO_DEFINE"

# On passe :
# - le fichier de conf
# - le sous-répertoire à exécuter
JAVA_ARGS="-DtnrConfigFile=${CONF_FILE} -Dtnr.logs.dir=${RUN_DIR} -DSCENARIOS=${SCENARIOS_SUBDIR}"

set +e
java ${JAVA_ARGS} -cp "${CP}" "${MAIN_CLASS}" 2>&1 | tee -a "${LOG_FILE}"
RC=${PIPESTATUS[0]}
set -e

echo "[INFO] ExitCode=${RC}" | tee -a "${LOG_FILE}"
exit "${RC}"
