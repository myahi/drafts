nohup ${JAVA_HOME}/bin/java \
  -Dlogging.config=/serveur_apps/tradeWebPostTrade/conf/logback-spring.xml \
  -jar ../lib/fixEngineTradeWeb.jar \
  /serveur_apps/tradeWebPostTrade/conf/sessionsSettings.txt \
  --spring.config.additional-location=/serveur_apps/tradeWebPostTrade/conf/application.properties \
  >> /serveur_apps/tradeWebPostTrade/logs/tradeWebPostTrade.log 2>&1 &
