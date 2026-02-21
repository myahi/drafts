#!/usr/bin/env bash
set -euo pipefail

EASYTNR_HOME="/serveur_apps/easy-tnr"

# ENV_NAME doit être fourni par le script appelant (start_batch.sh)
: "${ENV_NAME:?ENV_NAME is required (ex: DEVEAI)}"

RUNTIME_DIR="${EASYTNR_HOME}/${ENV_NAME}"

if [[ ! -d "${RUNTIME_DIR}" ]]; then
  echo "[ERROR] Runtime introuvable: ${RUNTIME_DIR}"
  exit 2
fi

CP=""

# Tous les jars directement dans le répertoire d'env (comme ta capture Windows)
for j in "${RUNTIME_DIR}"/*.jar; do
  [[ -e "$j" ]] || continue
  CP="${CP}:$j"
done

# Ajout config/resources de l'env (comme sous Windows)
if [[ -d "${RUNTIME_DIR}/config" ]]; then
  CP="${CP}:${RUNTIME_DIR}/config"
fi
if [[ -d "${RUNTIME_DIR}/resources" ]]; then
  CP="${CP}:${RUNTIME_DIR}/resources"
fi

export EASYTNR_HOME
export EASYTNR_RUNTIME_DIR="${RUNTIME_DIR}"
export EASYTNR_CP="${CP#:}"
