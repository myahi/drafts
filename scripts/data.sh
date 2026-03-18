#!/usr/bin/env bash
set -u

APP_ROOT="/serveur_apps/tibco/tra/domain/LBPEAI_REC_5_14/application"

matches_dir() {
  local d_lower
  d_lower="$(echo "$1" | tr '[:upper:]' '[:lower:]')"

  [[ "$d_lower" == *bofi_batch* && "$d_lower" == *integration* ]] \
  || [[ "$d_lower" == *maestro* && "$d_lower" == *integration* ]] \
  || [[ "$d_lower" == *eai_reports* && "$d_lower" == *integration* ]] \
  || [[ "$d_lower" == *eaimarketdata* && "$d_lower" == *integration* ]] \
  || [[ "$d_lower" == *eaiftp* && "$d_lower" == *integration* ]] \
  || [[ "$d_lower" == *eairef* && "$d_lower" == *integration* ]]
}

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

find "$APP_ROOT" -mindepth 1 -maxdepth 1 -type d | sort | while IFS= read -r dir
do
  base_dir="$(basename "$dir")"

  if matches_dir "$base_dir"; then
    echo "=================================================="
    echo "Répertoire : $dir"

    find "$dir" -maxdepth 1 -type f -name "*.sh" | sort | while IFS= read -r sh
    do
      sh_name="$(basename "$sh")"

      if is_running "$base_dir" "$sh_name"; then
        echo "Déjà en cours, stop/start : $sh_name"

        pids="$(get_pids "$base_dir" "$sh_name")"

        if [[ -n "$pids" ]]; then
          echo "Arrêt SIGTERM : $pids"
          for pid in $pids; do
            kill -15 "$pid"
          done

          sleep 5

          for pid in $pids; do
            if ps -p "$pid" >/dev/null 2>&1; then
              echo "Toujours actif, SIGKILL : $pid"
              kill -9 "$pid"
            else
              echo "Arrêt propre OK : $pid"
            fi
          done
        fi

        echo "Redémarrage : $sh"
        (
          cd "$dir" || exit 1
          nohup "$sh" >/dev/null 2>&1 &
        )
      else
        echo "Non lancé, ignoré : $sh_name"
      fi
    done
  fi
done
