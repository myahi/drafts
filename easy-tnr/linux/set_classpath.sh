#!/usr/bin/env bash
set -euo pipefail

EASYTNR_HOME="$(cd "$(dirname "$0")/.." && pwd)"
LIB_DIR="${EASYTNR_HOME}/libs"

CP=""

for j in "${LIB_DIR}"/*.jar; do
  [[ -e "$j" ]] || continue
  CP="${CP}:$j"
done

# ajoute config si ton app lit des .properties depuis le classpath
CP="${CP}:${EASYTNR_HOME}/config"

export EASYTNR_HOME
export EASYTNR_CP="${CP#:}"
