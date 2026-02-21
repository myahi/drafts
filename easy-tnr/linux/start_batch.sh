#!/usr/bin/env bash
set -euo pipefail

EASYTNR_HOME="/serveur_apps/easy-tnr"
LOGS_ROOT="${EASYTNR_HOME}/logs"

usage() {
  echo "Usage:"
  echo "  $0 --env DEVEAI --scenarios 'MAESTRO/GLMX_TO_CALYPSO/REPO'"
  echo ""
  echo "Optional:"
  echo "  --conf /chemin/vers/xxx.properties   (override conf)"
  exit 2
}

ENV_NAME=""
SCENARIOS_SUBDIR=""
CONF_FILE=""

while [[ $# -gt 0 ]]; do
  case "$1" in
    --env) ENV_NAME="$2"; shift 2;;
    --scenarios) SCENARIOS_SUBDIR="$2"; shift 2;;
    --conf) CONF_FILE="$2"; shift 2;;
    *) echo "Unknown arg: $1"; usage;;
  esac
done

[[ -n "${ENV_NAME}" && -n "${SCENARIOS_SUBDIR}" ]] || usage

export ENV_NAME

# Normalise Windows -> Linux
SCENARIOS_SUBDIR="${SCENARIOS_SUBDIR//\\//}"

RUNTIME_DIR="${EASYTNR_HOME}/${ENV_NAME}"
[[ -d "${RUNTIME_DIR}" ]] || { echo "[ERROR] Env dir introuvable: ${RUNTIME_DIR}"; exit 2; }

# Convention conf : dans le env/config/
# Si ton fichier s'appelle DEVEai.tnrConfigFile.properties (casse spéciale), tu peux soit:
# - le renommer en DEVEAI.tnrConfigFile.properties
# - ou passer --conf explicitement
if [[ -z "${CONF_FILE}" ]]; then
  # Convention simple (recommandée)
  C1="${RUNTIME_DIR}/config/${ENV_NAME}.tnrConfigFile.properties"
  # Fallback compatible avec ton nom actuel (DEVEai...)
  C2="${RUNTIME_DIR}/config/DEVEai.tnrConfigFile.properties"

  if [[ -f "${C1}" ]]; then
    CONF_FILE="${C1}"
  elif [[ -f "${C2}" ]]; then
    CONF_FILE="${C2}"
  else
    echo "[ERROR] Aucun fichier conf trouvé (essayé: ${C1} et ${C2})."
    echo "        -> passe --conf /chemin/vers/le/properties"
    exit 2
  fi
fi

[[ -f "${CONF_FILE}" ]] || { echo "[ERROR] Conf introuvable: ${CONF_FILE}"; exit 2; }

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
source "${SCRIPT_DIR}/set_classpath.sh"

TS="$(date +%Y%m%d_%H%M%S)"
RUN_DIR="${LOGS_ROOT}/${ENV_NAME}/$(echo "${SCENARIOS_SUBDIR}" | tr '/' '_')/${TS}"
mkdir -p "${RUN_DIR}"
LOG_FILE="${RUN_DIR}/run.log"

echo "====================================" | tee -a "${LOG_FILE}"
echo " EASYTNR BATCH" | tee -a "${LOG_FILE}"
echo " ENV      = ${ENV_NAME}" | tee -a "${LOG_FILE}"
echo " CONF     = ${CONF_FILE}" | tee -a "${LOG_FILE}"
echo " SCENARIO = ${SCENARIOS_SUBDIR}" | tee -a "${LOG_FILE}"
echo " LOG      = ${LOG_FILE}" | tee -a "${LOG_FILE}"
echo "====================================" | tee -a "${LOG_FILE}"

# ⚠️ à remplacer par le vrai main class (ou passer en -jar quand tu me donnes la commande Windows)
MAIN_CLASS="${MAIN_CLASS:-TO_DEFINE}"
if [[ "${MAIN_CLASS}" == "TO_DEFINE" ]]; then
  echo "[ERROR] MAIN_CLASS non défini. Ex: export MAIN_CLASS='xxx.yyy.Main'" | tee -a "${LOG_FILE}"
  exit 2
fi

JAVA_ARGS="-DtnrConfigFile=${CONF_FILE} \
-Dtnr.logs.dir=${RUN_DIR} \
-DSCENARIOS=${SCENARIOS_SUBDIR} \
-DENV=${ENV_NAME}"

set +e
java ${JAVA_ARGS} -cp "${EASYTNR_CP}" "${MAIN_CLASS}" 2>&1 | tee -a "${LOG_FILE}"
RC=${PIPESTATUS[0]}
set -e

echo "ExitCode=${RC}" | tee -a "${LOG_FILE}"
exit ${RC}
