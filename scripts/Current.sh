#!/usr/bin/env bash
set -u

APP_ROOT="/serveur_apps/tibco/tra/domain/LBPEAI_REC_5_14/application"
TMP_FILE="/tmp/restart_running_apps_$$.lst"

is_running() {
  local base_dir="$1"
  local sh_name="$2"
  ps -ef | grep -i -F "$base_dir" | grep -i -F "$sh_name" | grep -v grep >/dev/null 2>&1
}

get_pids() {
  local base_dir="$1"
  local sh_name="$2"
  ps -ef | grep -i -F "$base_dir" | grep -i -F "$sh_name" | grep -v grep | awk '{print $2}'
}

: > "$TMP_FILE"

find "$APP_ROOT" -mindepth 1 -maxdepth 1 -type d | sort | while IFS= read -r dir
do
  base_dir="$(basename "$dir")"

  find "$dir" -maxdepth 1 -type f -name "*.sh" | sort | while IFS= read -r sh
  do
    sh_name="$(basename "$sh")"

    if is_running "$base_dir" "$sh_name"; then
      echo "$dir|$sh" >> "$TMP_FILE"
      echo "Déjà en cours avant lancement du script : $sh"
    fi
  done
done

if [[ ! -s "$TMP_FILE" ]]; then
  echo "Aucun script en cours trouvé avant lancement."
  rm -f "$TMP_FILE"
  exit 0
fi

echo "=================================================="
echo "Arrêt des applications qui tournaient déjà"

while IFS='|' read -r dir sh
do
  base_dir="$(basename "$dir")"
  sh_name="$(basename "$sh")"
  pids="$(get_pids "$base_dir" "$sh_name")"

  if [[ -n "$pids" ]]; then
    echo "Arrêt SIGTERM de $sh_name : $pids"
    for pid in $pids
    do
      kill -15 "$pid"
    done

    sleep 5

    for pid in $pids
    do
      if ps -p "$pid" >/dev/null 2>&1; then
        echo "Toujours actif, SIGKILL : $pid"
        kill -9 "$pid"
      else
        echo "Arrêt propre OK : $pid"
      fi
    done
  fi
done < "$TMP_FILE"

echo "=================================================="
echo "Relance des applications qui tournaient déjà"

while IFS='|' read -r dir sh
do
  echo "Relance : $sh"
  (
    cd "$dir" || exit 1
    nohup "$sh" >/dev/null 2>&1 &
  )
done < "$TMP_FILE"

rm -f "$TMP_FILE"
echo "Terminé."
