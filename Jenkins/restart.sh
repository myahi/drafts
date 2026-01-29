#!/usr/bin/env bash
set -euo pipefail

APP_HOME="$(cd "$(dirname "$0")/.." && pwd)"
COMMON="/serveur_apps/eai-camel-ops/bin/appctl.sh"
JAR="$APP_HOME/lib/app.jar"
PROPS="$APP_HOME/config/application.properties"
LOGBACK="$APP_HOME/config/logback-spring.xml"
PROFILE="${1:-prod}"   # restart.sh [profile]

exec "$COMMON" restart \
  --name "eai-camel-rgv" \
  --run "$APP_HOME/run" \
  --jar "$JAR" \
  --props "$PROPS" \
  --logback "$LOGBACK" \
  --profile "$PROFILE"
~
