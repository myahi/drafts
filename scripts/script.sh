#!/usr/bin/env bash
set -u

APP_ROOT="/serveur_apps/tibco/tra/domain/LBPEAI_REC_5_14/application"

matches_dir() {
  local d="$1"
  [[ "$d" == *BOFI_BATCH* && "$d" == *INTEGRATION* ]] \
  || [[ "$d" == *MAESTRO* && "$d" == *INTEGRATION* ]] \
  || [[ "$d" == *EAI_REPORTS* && "$d" == *INTEGRATION* ]] \
  || [[ "$d" == *EAIMarketData* && "$d" == *INTEGRATION* ]] \
  || [[ "$d" == *EAIftp* && "$d" == *INTEGRATION* ]] \
  || [[ "$d" == *EAIRef* && "$d" == *INTEGRATION* ]]
}

find "$APP_ROOT" -mindepth 1 -maxdepth 1 -type d | while IFS= read -r dir
do
  base_dir="$(basename "$dir")"

  if matches_dir "$base_dir"; then
    echo "=================================================="
    echo "Répertoire : $dir"

    find "$dir" -maxdepth 1 -type f -name "*.sh" | sort | while IFS= read -r sh
    do
      sh_name="$(basename "$sh")"
      echo "Script trouvé : $sh_name"

      pids="$(ps -ef | grep -F "$base_dir" | grep -F "$sh_name" | grep -v grep | awk '{print $2}')"

      if [[ -n "$pids" ]]; then
        echo "Arrêt des process (SIGTERM) : $pids"
        for pid in $pids; do
          kill -15 "$pid"
        done

        sleep 5

        for pid in $pids; do
          if ps -p "$pid" > /dev/null 2>&1; then
            echo "Force kill (SIGKILL) : $pid"
            kill -9 "$pid"
          else
            echo "Arrêt propre OK : $pid"
          fi
        done
      else
        echo "Aucun process trouvé pour $sh_name"
      fi

      echo "Lancement : $sh"
      (
        cd "$dir" || exit 1
        nohup "$sh" >/dev/null 2>&1 &
      )
    done
  fi
done
