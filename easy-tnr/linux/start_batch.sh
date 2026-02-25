#!/usr/bin/env bash
set -euo pipefail

EASY_TNR_HOME="/serveur_apps/easy-tnr"

JAVA_HOME="/serveur_apps/tools/jdk1.8.0"
JAVA_EXE="${JAVA_HOME}/bin/java"

if [[ $# -lt 3 ]]; then
  echo "Usage:"
  echo "  $0 <ENV_NAME> <BATCH_NAME> <SCENARIOS> [VM_ARGS...]"
  echo ""
  echo "Example:"
  echo "  $0 DEVEAI MAESTRO_GLMX_REPO 'MAESTRO/GLMX_TO_CALYPSO/REPO' -DMaestroDBURL=..."
  exit 2
fi

ENV_NAME="$1"
BATCH_NAME="$2"
SCENARIOS="$3"
shift 3
VM_ARGS=("$@")

ENV_DIR="${EASY_TNR_HOME}/${ENV_NAME}"
CONFIG_FILE="${ENV_DIR}/config/${ENV_NAME}.tnrConfigFile.properties"
LOGGC_DIR="${ENV_DIR}/logs"

if [[ ! -x "${JAVA_EXE}" ]]; then
  echo "[ERROR] Java introuvable ou non exécutable: ${JAVA_EXE}"
  exit 2
fi

if [[ ! -d "${ENV_DIR}" ]]; then
  echo "[ERROR] ENV_DIR introuvable: ${ENV_DIR}"
  exit 2
fi

if [[ ! -f "${CONFIG_FILE}" ]]; then
  echo "[ERROR] Config file introuvable: ${CONFIG_FILE}"
  exit 2
fi

mkdir -p "${LOGGC_DIR}"

echo "===================================="
echo " Starting Easy TNR (Linux - Java 8)"
echo " JAVA_EXE     = ${JAVA_EXE}"
echo " ENV_DIR      = ${ENV_DIR}"
echo " CONFIG_FILE  = ${CONFIG_FILE}"
echo " BATCH_NAME   = ${BATCH_NAME}"
echo " SCENARIOS    = ${SCENARIOS}"
echo "===================================="


CLASSPATH="${ENV_DIR}/resources:${ENV_DIR}/*"

"${JAVA_EXE}" \
  -Dfile.encoding=UTF-8 \
  -Dsun.jnu.encoding=UTF-8 \
  -Duser.home="${ENV_DIR}" \
  "${VM_ARGS[@]}" \
  -Dtnr.configFile="${CONFIG_FILE}" \
  -Djava.util.Arrays.useLegacyMergeSort=true \
  -Xmx8g \
  -Xms2g \
  -Xss1m \
  -XX:MaxMetaspaceSize=1024m \
  -XX:+UseG1GC \
  -Dsun.rmi.transport.tcp.handshakeTimeout=1200000 \
  -Dsun.rmi.dgc.client.gcInterval=3600000 \
  -Xloggc:"${LOGGC_DIR}/${BATCH_NAME}_LOGGC.log" \
  -XX:+PrintGCDetails \
  -XX:+PrintGCDateStamps \
  -XX:+PrintGCTimeStamps \
  -verbose:gc \
  -cp "${CLASSPATH}" \
  lbp.qa.easy.tnr.batch.EasyTnrLauncherBatch \
  -batchName "${BATCH_NAME}" \
  -scenarios "${SCENARIOS}"
