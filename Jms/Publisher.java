package com.mycompany.eai.camel.core.jms;

public class JmsPublishException extends RuntimeException {
    public JmsPublishException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.mycompany.eai.camel.core.jms;

import org.apache.camel.ProducerTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class GenericJmsPublisher {

    private final ProducerTemplate producer;

    public GenericJmsPublisher(ProducerTemplate producer) {
        this.producer = producer;
    }

    public void sendToQueue(String queueName, Object body) {
        sendToQueue(queueName, body, null);
    }

    public void sendToQueue(String queueName, Object body, Map<String, Object> headers) {
        send("jms:queue:" + queueName, body, headers);
    }

    public void sendToTopic(String topicName, Object body) {
        sendToTopic(topicName, body, null);
    }

    public void sendToTopic(String topicName, Object body, Map<String, Object> headers) {
        send("jms:topic:" + topicName, body, headers);
    }

    public void sendToEndpoint(String endpointUri, Object body) {
        sendToEndpoint(endpointUri, body, null);
    }

    public void sendToEndpoint(String endpointUri, Object body, Map<String, Object> headers) {
        send(endpointUri, body, headers);
    }

    private void send(String endpointUri, Object body, Map<String, Object> headers) {
        try {
            if (headers == null || headers.isEmpty()) {
                producer.sendBody(endpointUri, body);
            } else {
                producer.sendBodyAndHeaders(endpointUri, body, headers);
            }
        } catch (Exception e) {
            throw new JmsPublishException("Error publishing message to " + endpointUri, e);
        }
    }
}
