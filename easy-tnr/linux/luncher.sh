#!/usr/bin/env bash
set -euo pipefail

EASY_TNR_DIRECTORY="/serveur_apps/easy-tnr"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

usage() {
  echo "Usage:"
  echo "  $0 <ENV> start-batch <BATCH_NAME> <SCENARIOS> [VM_PARAMS...]"
  echo ""
  echo "Example:"
  echo "  $0 DEVEAI start-batch MAESTRO_GLMX_REPO 'MAESTRO/GLMX_TO_CALYPSO/REPO' -Dfoo=bar"
  exit 2
}

[[ $# -ge 4 ]] || usage

ENV_NAME="$1"; shift
COMMAND="$1"; shift

if [[ "${COMMAND}" != "start-batch" ]]; then
  echo "[ERROR] Commande non supportée: ${COMMAND} (supporté: start-batch)"
  exit 2
fi

BATCH_NAME="$1"; shift
SCENARIOS="$1"; shift
# Le reste = VM_PARAMS (peut contenir plein de -D...)
VM_PARAMS=("$@")

# Normalise \ -> / pour être compatible Jenkins/Windows
SCENARIOS="${SCENARIOS//\\//}"

export ENV_NAME

# Date comme dans le cmd : currentDate YYYYMMDD / currentDateyyMMdd YYMMDD
CURRENT_DATE="$(date +%Y%m%d)"
CURRENT_DATE_YYMMDD="$(date +%y%m%d)"

# logs
LOG_ROOT="${EASY_TNR_DIRECTORY}/logs"
RUN_DIR="${LOG_ROOT}/${ENV_NAME}/${BATCH_NAME}/$(date +%Y%m%d_%H%M%S)"
mkdir -p "${RUN_DIR}"
LOG_FILE="${RUN_DIR}/run.log"

echo "" | tee -a "${LOG_FILE}"
echo "JAVA=$(command -v java || true)" | tee -a "${LOG_FILE}"
echo "ENV=${ENV_NAME}" | tee -a "${LOG_FILE}"
echo "BATCH_NAME=${BATCH_NAME}" | tee -a "${LOG_FILE}"
echo "SCENARIOS=${SCENARIOS}" | tee -a "${LOG_FILE}"

# classpath
source "${SCRIPT_DIR}/set_classpath.sh"

# IMPORTANT :
# Ici il faut la vraie façon de lancer l’app (main class ou -jar).
# On ne l’a pas dans l’extrait RUN_TNR_SCENARIO, elle est dans le launcher.cmd original.
# Donc je te donne 2 modes : A) -jar auto si un jar exécutable existe, sinon B) MAIN_CLASS via env var.

# A) jar exécutable auto (Main-Class dans manifest)
find_exec_jar() {
  local dir="$1"
  local j
  for j in "$dir"/*.jar; do
    [[ -e "$j" ]] || continue
    if unzip -p "$j" META-INF/MANIFEST.MF 2>/dev/null | grep -qi '^Main-Class:'; then
      echo "$j"
      return 0
    fi
  done
  return 1
}

EXEC_JAR="$(find_exec_jar "${EASYTNR_BASE_DIR}" || true)"

# VM params finaux (ce que fait le cmd)
FINAL_VM_PARAMS=("${VM_PARAMS[@]}" "-DcurrentDate=${CURRENT_DATE}" "-DcurrentDateyyMMdd=${CURRENT_DATE_YYMMDD}" "-DBATCH_NAME=${BATCH_NAME}" "-DSCENARIOS=${SCENARIOS}" "-Dtnr.logs.dir=${RUN_DIR}")

echo "VM_PARAMS=${FINAL_VM_PARAMS[*]}" | tee -a "${LOG_FILE}"
echo "" | tee -a "${LOG_FILE}"

set +e
if [[ -n "${EXEC_JAR}" ]]; then
  echo "[INFO] Launch mode: -jar ${EXEC_JAR}" | tee -a "${LOG_FILE}"
  java "${FINAL_VM_PARAMS[@]}" -jar "${EXEC_JAR}" 2>&1 | tee -a "${LOG_FILE}"
  RC=${PIPESTATUS[0]}
else
  # B) main class explicit
  : "${MAIN_CLASS:?MAIN_CLASS non défini et aucun jar exécutable détecté}"
  echo "[INFO] Launch mode: -cp ... ${MAIN_CLASS}" | tee -a "${LOG_FILE}"
  java "${FINAL_VM_PARAMS[@]}" -cp "${EASYTNR_CLASSPATH}" "${MAIN_CLASS}" 2>&1 | tee -a "${LOG_FILE}"
  RC=${PIPESTATUS[0]}
fi
set -e

echo "ExitCode=${RC}" | tee -a "${LOG_FILE}"
exit "${RC}"
