package com.mycompany.app.camel;

import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class MqRoutes extends RouteBuilder {

    @Override
    public void configure() {

        // =========================
        // Consumer MQ
        // =========================
        from("mqJms:queue:QUEUE.IN?concurrentConsumers=3")
            .routeId("mq-consumer-queue-in")
            .log("MQ IN message received: ${body}")
            .to("direct:processMessage");

        // =========================
        // Traitement métier
        // =========================
        from("direct:processMessage")
            .routeId("process-message")
            .log("Processing message: ${body}")
            // traitement métier ici
            .to("direct:sendToMq");

        // =========================
        // Producer MQ
        // =========================
        from("direct:sendToMq")
            .routeId("mq-producer-queue-out")
            .setHeader("JMSCorrelationID", simple("${exchangeId}"))
            .to("mqJms:queue:QUEUE.OUT")
            .log("MQ OUT message sent, correlationId=${header.JMSCorrelationID}");
    }
}
