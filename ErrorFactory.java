import org.apache.camel.Exchange;
import org.apache.camel.Message;

import java.io.PrintWriter;
import java.io.StringWriter;

public final class ErrorFactory {

  private ErrorFactory() {}

  public static com.mycompany.model.Error fromExchange(Exchange exchange, Exception ex) {

    com.mycompany.model.Error err = new com.mycompany.model.Error();

    // --- ProcessContext ---
    com.mycompany.model.Error.ProcessContext pc = new com.mycompany.model.Error.ProcessContext();
    pc.setProcessId(System.currentTimeMillis()); // ou un vrai id métier si tu en as un
    pc.setProjectName("camel-spring-boot");      // ou depuis properties
    pc.setEngineName("soap-server");            // ou hostname/appname
    pc.setRestartedFromCheckpoint(false);
    pc.getTrackingInfo().add("exchangeId=" + exchange.getExchangeId());
    pc.getTrackingInfo().add("routeId=" + exchange.getFromRouteId());
    pc.setCustomId(exchange.getIn().getHeader("X-CUSTOM-ID", String.class));

    // --- ErrorReport ---
    com.mycompany.model.Error.ErrorReport er = new com.mycompany.model.Error.ErrorReport();
    er.setMsg(ex.getMessage() != null ? ex.getMessage() : "Unexpected error");
    er.setFullClass(ex.getClass().getName());
    er.setClazz(ex.getClass().getSimpleName());
    er.setProcessStack(exchange.getFromRouteId());

    er.setStackTrace(stackTrace(ex));
    er.setMsgCode("ERR-500"); // si tu as un mapping, tu peux le faire ici

    // --- References (0..n) ---
    err.getReference().add(new com.mycompany.model.Error.Reference("APP-001", "MY_APP"));

    // --- Metadatas (ns2) ---
    com.mycompany.model.Error.Metadatas metas = new com.mycompany.model.Error.Metadatas();
    metas.getMetadata().add(new com.mycompany.model.Error.Metadata("timestamp", String.valueOf(System.currentTimeMillis())));
    metas.getMetadata().add(new com.mycompany.model.Error.Metadata("exchangeId", exchange.getExchangeId()));
    if (exchange.getFromRouteId() != null) {
      metas.getMetadata().add(new com.mycompany.model.Error.Metadata("routeId", exchange.getFromRouteId()));
    }
    err.setMetadatas(metas);

    err.setProcessContext(pc);
    err.setErrorReport(er);

    return err;
  }

  private static String stackTrace(Throwable t) {
    StringWriter sw = new StringWriter();
    t.printStackTrace(new PrintWriter(sw));
    return sw.toString();
  }
}
