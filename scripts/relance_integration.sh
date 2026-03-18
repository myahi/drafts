via un script shell, je veux lister les repertoire disponible sur ce chemin 
/serveur_apps/tibco/tra/domain/LBPEAI_REC_5_14/application/
pui lister les repertoire dont le nom contient
BOFI_BATCH et INTEGRATION
MAESTRO et INTEGRATION
EAI_REPORTS et INTEGRATION
EAIMarketData et INTEGRATION
EAIftp et INTEGRATION
EAIRef et INTEGRATION

pour chaque repertoire trouvé on doit lister chaque couple:
.tra  et .sh 

puis lancer le .sh avec le tra en paremetre:

exemple de commande

/serveur_apps/tibco/bw/5.14/bin/bwengine --pid --run --propFile /serveur_apps/tibco//tra/domain/LBPEAI_REC_5_14/application/MAESTRO-UX-INTEGRATION/MAESTRO-UX-INTEGRATION-360TradesToCalypso_INTEGRATION.tra --innerProcess
