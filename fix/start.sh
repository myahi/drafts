nohup ${JAVA_HOME}/bin/java \
-Dlog4j2.configurationFile=/serveur_apps/tradeWebPostTrade/conf/log4j2.xml \
-jar ../lib/fixEngineTradeWeb.jar \
/serveur_apps/tradeWebPostTrade/conf/sessionsSettings.txt \
--spring.config.location=/serveur_apps/tradeWebPostTrade/conf/application.properties \
 2>>/serveur_apps/tradeWebPostTrade/logs/tradeWebPostTrade.log &
