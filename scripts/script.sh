#!/usr/bin/env bash
set -u

APP_ROOT="/serveur_apps/tibco/tra/domain/LBPEAI_REC_5_14/application"
BWENGINE="/serveur_apps/tibco/bw/5.14/bin/bwengine"

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
    echo "Répertoire trouvé : $dir"

    sh_files=()
    tra_files=()

    while IFS= read -r f; do
      sh_files+=("$f")
    done < <(find "$dir" -maxdepth 1 -type f -name "*.sh" | sort)

    while IFS= read -r f; do
      tra_files+=("$f")
    done < <(find "$dir" -maxdepth 1 -type f -name "*.tra" | sort)

    echo ".sh trouvés :"
    for sh in "${sh_files[@]}"; do
      echo "  - $(basename "$sh")"
    done

    echo ".tra trouvés :"
    for tra in "${tra_files[@]}"; do
      echo "  - $(basename "$tra")"
    done

    echo "Couples .sh / .tra :"
    for sh in "${sh_files[@]}"; do
      sh_base="$(basename "$sh" .sh)"
      for tra in "${tra_files[@]}"; do
        tra_base="$(basename "$tra" .tra)"
        if [[ "$sh_base" == "$tra_base" ]]; then
          echo "  - $(basename "$sh") <-> $(basename "$tra")"

          echo "Lancement : $BWENGINE --pid --run --propFile $tra --innerProcess"
          (
            cd "$dir" || exit 1
            "$BWENGINE" --pid --run --propFile "$tra" --innerProcess
          )
        fi
      done
    done
  fi
done
