package fr.lbp.routes;

import fr.lbp.lib.audit.TibcoAuditHelper;
import fr.lbp.lib.model.audit.Audit;
import fr.lbp.lib.model.auditinfo.AuditInfo;
import fr.lbp.lib.model.auditinfo.Metadata;
import fr.lbp.lib.model.auditinfo.Metadatas;
import fr.lbp.lib.model.auditinfo.Reference;
import fr.lbp.lib.model.enginetypes.ProcessContext;
import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class OrderProcessingRouteWithoutBuilder extends RouteBuilder {
    
    @Autowired
    private TibcoAuditHelper auditHelper;
    
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = 
        DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ssXXX");
    
    @Override
    public void configure() {
        
        from("cxf:bean:soapEndpoint")
            .routeId("order-processing-route")
            
            // ============ INITIALISATION ============
            .process(exchange -> {
                // Créer le ProcessContext manuellement (SANS @Builder)
                ProcessContext ctx = new ProcessContext();
                ctx.setProcessId(System.currentTimeMillis());
                ctx.setProjectName("OrderManagement");
                ctx.setEngineName("order-processing-route");
                ctx.setRestartedFromCheckpoint(false);
                ctx.setCustomId("EXEC-" + System.currentTimeMillis());
                
                // Ajouter des trackingInfo
                ctx.getTrackingInfo().add("userId:" + exchange.getIn().getHeader("userId"));
                ctx.getTrackingInfo().add("source:SOAP");
                
                // Initialiser dans l'exchange
                auditHelper.initProcessContext(exchange, ctx);
                exchange.setProperty("startTime", System.currentTimeMillis());
            })
            
            // ============ OPTION 1: Utiliser le helper (recommandé) ============
            .process(exchange -> {
                String xmlInput = exchange.getIn().getBody(String.class);
                
                auditHelper.audit(exchange)
                    .desc("Réception message SOAP")
                    .status("RECEIVED")
                    .data(xmlInput)
                    .meta("messageSize", xmlInput.length())
                    .meta("contentType", "application/xml")
                    .send();
            })
            
            .unmarshal().jaxb()
            
            // ============ OPTION 2: Créer l'audit manuellement (sans helper) ============
            .process(exchange -> {
                Order order = exchange.getIn().getBody(Order.class);
                
                // Créer AuditInfo manuellement
                AuditInfo auditInfo = new AuditInfo();
                auditInfo.setTimestamp(ZonedDateTime.now().format(TIMESTAMP_FORMATTER));
                auditInfo.setDescription("Validation commande");
                auditInfo.setData(order.toString());
                auditInfo.setStatut("VALIDATING");
                
                // Ajouter des références
                List<Reference> references = new ArrayList<>();
                Reference ref1 = new Reference();
                ref1.setName("orderId");
                ref1.setValue(order.getId());
                references.add(ref1);
                
                Reference ref2 = new Reference();
                ref2.setName("customerId");
                ref2.setValue(order.getCustomerId());
                references.add(ref2);
                
                auditInfo.setReference(references);
                
                // Ajouter des métadonnées
                Metadatas metadatasObj = new Metadatas();
                List<Metadata> metadataList = new ArrayList<>();
                
                Metadata meta1 = new Metadata("orderType", order.getType());
                metadataList.add(meta1);
                
                Metadata meta2 = new Metadata("amount", String.valueOf(order.getAmount()));
                metadataList.add(meta2);
                
                Metadata meta3 = new Metadata("exchangeId", exchange.getExchangeId());
                metadataList.add(meta3);
                
                metadatasObj.setMetadata(metadataList);
                auditInfo.setMetadatas(metadatasObj);
                
                // Récupérer le ProcessContext
                ProcessContext ctx = exchange.getProperty("processContext", ProcessContext.class);
                
                // Créer l'objet Audit final
                Audit audit = new Audit();
                audit.setAuditInfo(auditInfo);
                audit.setProcessContext(ctx);
                audit.setForceAudit(false);
                
                // Envoyer via le helper
                auditHelper.sendAudit(audit);
            })
            
            .to("direct:validation")
            
            // ============ OPTION 3: Mélange (helper + personnalisation) ============
            .process(exchange -> {
                Order order = exchange.getIn().getBody(Order.class);
                
                // Enrichir le ProcessContext existant
                ProcessContext ctx = exchange.getProperty("processContext", ProcessContext.class);
                ctx.getTrackingInfo().add("orderId:" + order.getId());
                ctx.getTrackingInfo().add("step:processing");
                
                // Utiliser le helper pour créer l'audit
                auditHelper.audit(exchange)
                    .desc("Traitement commande")
                    .status("PROCESSING")
                    .data(order)
                    .ref("orderId", order.getId())
                    .ref("customerId", order.getCustomerId())
                    .meta("totalAmount", order.getAmount())
                    .meta("currency", "EUR")
                    .meta("itemsCount", order.getItems().size())
                    .send();
            })
            
            .to("bean:orderService?method=process")
            
            // ============ Exemple avec ProcessContext personnalisé ============
            .process(exchange -> {
                Order order = exchange.getIn().getBody(Order.class);
                
                // Créer un ProcessContext spécifique pour cet audit
                ProcessContext specialCtx = new ProcessContext();
                specialCtx.setProcessId(System.currentTimeMillis());
                specialCtx.setProjectName("SpecialAudit");
                specialCtx.setEngineName("special-processor");
                specialCtx.setRestartedFromCheckpoint(false);
                specialCtx.setCustomId("SPECIAL-" + order.getId());
                specialCtx.getTrackingInfo().add("special:true");
                specialCtx.getTrackingInfo().add("priority:HIGH");
                
                // Utiliser ce ProcessContext pour cet audit uniquement
                auditHelper.audit(exchange)
                    .desc("Audit spécial avec contexte personnalisé")
                    .status("SPECIAL")
                    .data(order)
                    .processContext(specialCtx)
                    .meta("reason", "high-value-order")
                    .send();
            })
            
            // ============ Audit de fin ============
            .process(exchange -> {
                // Enrichir le ProcessContext global
                ProcessContext ctx = exchange.getProperty("processContext", ProcessContext.class);
                ctx.getTrackingInfo().add("step:completed");
                ctx.getTrackingInfo().add("endTime:" + System.currentTimeMillis());
                
                auditHelper.audit(exchange)
                    .desc("Traitement terminé avec succès")
                    .status("SUCCESS")
                    .data("Commande traitée et confirmée")
                    .forceAudit(true)
                    .send();
            })
            
            .marshal().jaxb();
        
        // ============ Gestion des erreurs ============
        onException(Exception.class)
            .process(exchange -> {
                Exception error = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
                
                // Enrichir le ProcessContext avec l'erreur
                ProcessContext ctx = exchange.getProperty("processContext", ProcessContext.class);
                if (ctx != null) {
                    ctx.getTrackingInfo().add("error:" + error.getClass().getSimpleName());
                    ctx.getTrackingInfo().add("errorMessage:" + error.getMessage());
                }
                
                auditHelper.audit(exchange)
                    .desc("Erreur technique")
                    .status("ERROR")
                    .data(error.getMessage())
                    .meta("errorClass", error.getClass().getSimpleName())
                    .meta("severity", "CRITICAL")
                    .forceAudit(true)
                    .send();
            })
            .handled(true);
        
        // ============ Route de traitement des audits ============
        from("seda:audit?concurrentConsumers=5&size=1000")
            .routeId("audit-processor")
            .log("Audit reçu: ${body.auditInfo.description}")
            // Marshaller en XML
            .marshal().jaxb()
            .log("Audit XML: ${body}")
            // Envoyer vers votre système d'audit
            .to("file:///opt/audit?fileName=audit-${date:now:yyyyMMdd-HHmmss}.xml")
            // Ou vers JMS
            // .to("jms:queue:AUDIT.QUEUE")
            // Ou vers une base de données
            // .to("jpa:AuditEntity")
            ;
    }
}
