#!/usr/bin/env bash
set -euo pipefail

EASY_TNR_DIRECTORY="/serveur_apps/easy-tnr"

: "${ENV_NAME:?ENV_NAME manquant}"

# Layout Windows: easy-tnr/libs/<ENV>/...
LIB_ENV_DIR="${EASY_TNR_DIRECTORY}/libs/${ENV_NAME}"

# Fallback si vos jars sont ailleurs
if [[ -d "${LIB_ENV_DIR}" ]]; then
  BASE_DIR="${LIB_ENV_DIR}"
else
  # fallback possible: easy-tnr/DEVEAI (si tu as tout dedans)
  ALT_DIR="${EASY_TNR_DIRECTORY}/${ENV_NAME}"
  if [[ -d "${ALT_DIR}" ]]; then
    BASE_DIR="${ALT_DIR}"
  else
    echo "[ERROR] Impossible de trouver le runtime de l'env ${ENV_NAME}."
    echo "Cherché: ${LIB_ENV_DIR} ou ${ALT_DIR}"
    exit 2
  fi
fi

CP=""

# Jars à la racine de BASE_DIR
for j in "${BASE_DIR}"/*.jar; do
  [[ -e "$j" ]] || continue
  CP="${CP}:$j"
done

# Dossiers souvent nécessaires
[[ -d "${BASE_DIR}/resources" ]] && CP="${CP}:${BASE_DIR}/resources"
[[ -d "${BASE_DIR}/config" ]] && CP="${CP}:${BASE_DIR}/config"

# Et le config global (si vous l’utilisez)
[[ -d "${EASY_TNR_DIRECTORY}/config" ]] && CP="${CP}:${EASY_TNR_DIRECTORY}/config"

export EASY_TNR_DIRECTORY
export EASYTNR_BASE_DIR="${BASE_DIR}"
export EASYTNR_CLASSPATH="${CP#:}"
