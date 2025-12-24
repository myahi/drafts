package com.mycompany.routes;

import com.mycompany.audit.AuditHelper;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class OrderProcessingRoute extends RouteBuilder {
    
    @Autowired
    private AuditHelper auditHelper;
    
    @Override
    public void configure() {
        
        from("cxf:bean:soapEndpoint")
            .routeId("order-processing")
            
            // Initialiser le timer
            .process(exchange -> {
                exchange.setProperty("startTime", System.currentTimeMillis());
            })
            
            // Audit START simple
            .process(exchange -> {
                auditHelper.auditStart(exchange, exchange.getIn().getBody());
            })
            
            .unmarshal().jaxb()
            
            // Audit VALIDATION avec métadonnées
            .process(exchange -> {
                auditHelper.createAudit(exchange, "VALIDATION_START")
                    .status("IN_PROGRESS")
                    .metadata("validationType", "SCHEMA")
                    .metadata("schemaVersion", "1.2")
                    .send();
            })
            
            .to("direct:validation")
            
            .process(exchange -> {
                auditHelper.createAudit(exchange, "VALIDATION_END")
                    .status("SUCCESS")
                    .metadata("rulesApplied", 12)
                    .metadata("warningsCount", 0)
                    .send();
            })
            
            // Traitement métier
            .to("bean:orderService?method=process")
            
            // Audit PROCESSING avec payload
            .process(exchange -> {
                Order order = exchange.getIn().getBody(Order.class);
                
                auditHelper.createAudit(exchange, "PROCESSING")
                    .status("COMPLETED")
                    .payload(order)
                    .metadata("orderId", order.getId())
                    .metadata("totalAmount", order.getAmount())
                    .metadata("itemsCount", order.getItems().size())
                    .send();
            })
            
            // Audit END
            .process(exchange -> {
                auditHelper.auditEnd(exchange, exchange.getIn().getBody());
            })
            
            .marshal().jaxb();
        
        // Gestion des erreurs avec audit
        onException(Exception.class)
            .process(exchange -> {
                Exception exception = exchange.getProperty(
                    Exchange.EXCEPTION_CAUGHT, Exception.class);
                auditHelper.auditError(exchange, exception);
            })
            .handled(true)
            .setBody(constant("Error occurred"));
        
        // Route de traitement des audits
        from("seda:audit?concurrentConsumers=5&size=1000")
            .routeId("audit-processor")
            .log("Audit received: ${body}")
            .to("log:audit?level=INFO")
            // Persister en base de données
            .to("jpa:com.mycompany.entity.AuditEntity")
            // Envoyer à Kafka (optionnel)
            .to("kafka:audit-events?brokers=localhost:9092");
    }
}
