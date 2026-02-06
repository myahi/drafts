#!/usr/bin/env bash
set -euo pipefail

# Se placer dans le dossier où se trouve ce script (bin/)
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

CONF_FILE="../conf/application.properties"

usage() {
  echo "Usage: $(basename "$0") <couleur>"
  echo "Ex: $(basename "$0") rouge"
  exit 2
}

COLOR="${1:-}"
[[ -n "$COLOR" ]] || usage

# Normalisation: tout en minuscule
COLOR_LC="$(printf '%s' "$COLOR" | tr '[:upper:]' '[:lower:]')"
PREFIX="re7${COLOR_LC}"

if [[ ! -f "$CONF_FILE" ]]; then
  echo "ERROR: fichier introuvable: $CONF_FILE"
  echo "INFO : je suis positionné dans: $(pwd)"
  exit 1
fi

echo "[INFO] Using prefix: $PREFIX"
echo "[INFO] Conf file : $CONF_FILE"

# Stop best-effort
if [[ -x "./stop.sh" ]]; then
  echo "[INFO] Stopping..."
  ./stop.sh || true
else
  echo "[WARN] ./stop.sh non trouvé ou non exécutable (skip)"
fi

tmp="$(mktemp)"

awk -v pref="$PREFIX" '
  # lbp.ems-queue-name=<prefix>.<reste>  => lbp.ems-queue-name=pref.<reste>
  /^[[:space:]]*lbp\.ems-queue-name[[:space:]]*=/ {
    if (match($0, /^[[:space:]]*lbp\.ems-queue-name[[:space:]]*=[[:space:]]*[^.]*\.(.*)$/, m)) {
      $0 = "lbp.ems-queue-name=" pref "." m[1]
    }
  }

  # lbp.ems-error-queue-name=<prefix>.<reste> => lbp.ems-error-queue-name=pref.<reste>
  /^[[:space:]]*lbp\.ems-error-queue-name[[:space:]]*=/ {
    if (match($0, /^[[:space:]]*lbp\.ems-error-queue-name[[:space:]]*=[[:space:]]*[^.]*\.(.*)$/, m)) {
      $0 = "lbp.ems-error-queue-name=" pref "." m[1]
    }
  }

  { print }
' "$CONF_FILE" > "$tmp"

mv -f "$tmp" "$CONF_FILE"

echo "[INFO] New values:"
grep -E '^[[:space:]]*lbp\.ems-queue-name[[:space:]]*=|^[[:space:]]*lbp\.ems-error-queue-name[[:space:]]*=' "$CONF_FILE" || true

# Start
if [[ -x "./start.sh" ]]; then
  echo "[INFO] Starting..."
  ./start.sh
else
  echo "[WARN] ./start.sh non trouvé ou non exécutable"
  exit 1
fi

echo "[OK] Switch done."
