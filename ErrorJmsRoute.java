import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.Marshaller;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import java.io.StringWriter;

@Component
public class ErrorJmsRoute extends RouteBuilder {

  @Override
  public void configure() {

    from("direct:publishErrorJms")
      .routeId("publish-error-jms")
      .process(e -> {
        // récupérer l'objet construit dans le onException
        com.mycompany.model.Error errObj =
            e.getProperty("errorObj", com.mycompany.model.Error.class);

        // marshal JAXB -> XML string
        JAXBContext ctx = JAXBContext.newInstance(com.mycompany.model.Error.class);
        Marshaller marshaller = ctx.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

        StringWriter sw = new StringWriter();
        marshaller.marshal(errObj, sw);

        e.getMessage().setBody(sw.toString());
        e.getMessage().setHeader("Content-Type", "application/xml");
      })
      .setHeader("JMSType").constant("ERROR_XML")
      .setHeader("JMSCorrelationID", simple("${exchangeId}"))
      .to("jms:queue:ERROR.QUEUE");
  }
}
