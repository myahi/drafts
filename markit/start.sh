nohup "${JAVA_HOME}/bin/java" \
  -Dfile.encoding=UTF-8 \
  -Dlog4j2.configurationFile=file:///serveur_apps/markitDealertEngine/conf/log4j2.xml \
  -jar ../lib/markit_dealer_engine.jar \
  --spring.config.location=file:///serveur_apps/markitDealertEngine/conf/application.properties \
  >/serveur_apps/markitDealertEngine/logs/stdout.log 2>/serveur_apps/markitDealertEngine/logs/stderr.log &
echo $! > /serveur_apps/markitDealertEngine/run/markit.pid
