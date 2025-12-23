package com.mycompany.error;

import com.mycompany.model.AnyData;
import com.mycompany.model.Error;
import com.mycompany.model.ErrorReport;
import com.mycompany.model.Metadata;
import com.mycompany.model.Metadatas;
import com.mycompany.model.ProcessContext;
import com.mycompany.model.Reference;
import org.apache.camel.Exchange;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.InetAddress;
import java.time.Instant;
import java.util.UUID;

/**
 * Construit un objet {@link Error} (modèle JAXB "maison") à partir d'un Exchange Camel et d'une Exception.
 *
 * Objectif :
 * - Remplir ProcessContext + ErrorReport conformément aux XSD (processinfo.xsd)
 * - Optionnellement remplir les anydata (xs:any) avec un DOM Element
 * - Ajouter des références et metadatas (auditinfo.xsd)
 *
 * Remarque:
 * - Adapte projectName/engineName/msgCode selon ton contexte applicatif.
 */
public final class ErrorBuilder {

    // Codes/messages par défaut (à adapter)
    private static final String DEFAULT_PROJECT_NAME = "camel-spring-boot";
    private static final String DEFAULT_ENGINE_NAME = "soap-server";
    private static final String DEFAULT_MSG_CODE = "ERR-500";
    private static final String DEFAULT_REFERENCE_CODE = "APP-001";
    private static final String DEFAULT_REFERENCE_CODIFIER = "MY_APP";

    // Header(s) utiles
    private static final String HDR_CUSTOM_ID = "X-CUSTOM-ID";
    private static final String HDR_CORRELATION_ID = "X-CORRELATION-ID";
    private static final String HDR_OPERATION = "X-OPERATION";
    private static final String HDR_SERVICE = "X-SERVICE";

    private ErrorBuilder() {
        // utilitaire
    }

    /**
     * Construit l'objet Error complet (racine), prêt à être marshalé en XML et publié en JMS.
     *
     * @param exchange Exchange Camel
     * @param ex       Exception capturée (Exchange.EXCEPTION_CAUGHT)
     */
    public static Error build(Exchange exchange, Exception ex) {
        if (exchange == null) {
            throw new IllegalArgumentException("exchange must not be null");
        }
        if (ex == null) {
            ex = new RuntimeException("Unknown error");
        }

        String errorId = UUID.randomUUID().toString();

        Error err = new Error();
        err.setProcessContext(buildProcessContext(exchange, errorId));
        err.setErrorReport(buildErrorReport(exchange, ex, errorId));

        // Root <reference> (0..n) - à adapter (si tu en as plusieurs, ajoute-les)
        err.getReference().add(new Reference(DEFAULT_REFERENCE_CODE, DEFAULT_REFERENCE_CODIFIER));

        // ns2:metadatas (auditinfo.xsd)
        err.setMetadatas(buildMetadatas(exchange, ex, errorId));

        // Root <data> (anydata) optionnel (tu peux le désactiver si tu ne veux rien)
        // err.setData(new AnyData(buildRootAnyData(exchange, ex, errorId)));

        return err;
    }

    /**
     * Construit ProcessContext (processinfo.xsd)
     */
    public static ProcessContext buildProcessContext(Exchange exchange, String errorId) {
        ProcessContext pc = new ProcessContext();

        // Obligatoire (XSD)
        pc.setProcessId(System.currentTimeMillis());
        pc.setProjectName(resolveProjectName(exchange));
        pc.setEngineName(resolveEngineName());
        pc.setRestartedFromCheckpoint(false);

        // TrackingInfo (0..n)
        pc.getTrackingInfo().add("timestamp=" + Instant.now());
        pc.getTrackingInfo().add("errorId=" + errorId);
        pc.getTrackingInfo().add("exchangeId=" + exchange.getExchangeId());

        if (exchange.getFromRouteId() != null) {
            pc.getTrackingInfo().add("routeId=" + exchange.getFromRouteId());
        }
        if (exchange.getFromEndpoint() != null) {
            pc.getTrackingInfo().add("fromEndpoint=" + exchange.getFromEndpoint().getEndpointUri());
        }

        String corr = header(exchange, HDR_CORRELATION_ID);
        if (corr != null && !corr.isBlank()) {
            pc.getTrackingInfo().add("correlationId=" + corr);
        }

        // CustomId (0..1)
        pc.setCustomId(header(exchange, HDR_CUSTOM_ID));

        return pc;
    }

    /**
     * Construit ErrorReport (processinfo.xsd)
     */
    public static ErrorReport buildErrorReport(Exchange exchange, Exception ex, String errorId) {
        ErrorReport er = new ErrorReport();

        // Obligatoire (XSD)
        er.setStackTrace(stackTrace(ex));
        er.setMsg(safeMessage(ex));
        er.setFullClass(ex.getClass().getName());
        er.setClazz(ex.getClass().getSimpleName());
        er.setProcessStack(exchange.getFromRouteId() != null ? exchange.getFromRouteId() : "unknown");

        // Optionnel
        er.setMsgCode(resolveMsgCode(exchange, ex));

        // Optionnel: ErrorReport.Data (anydata)
        // On met un <details>...</details> pour audit technique
        er.setData(new AnyData(buildErrorReportAnyData(exchange, ex, errorId)));

        return er;
    }

    /**
     * Construit ns2:metadatas (auditinfo.xsd).
     *
     * Ton XSD auditinfo prévoit aussi auditInfo/timestamp/reference/description/data/statut,
     * mais ton XSD "error" ne référence que ns2:metadatas, donc on remplit uniquement metadatas.
     */
    public static Metadatas buildMetadatas(Exchange exchange, Exception ex, String errorId) {
        Metadatas metas = new Metadatas();

        metas.getMetadata().add(new Metadata("errorId", errorId));
        metas.getMetadata().add(new Metadata("timestamp", Instant.now().toString()));
        metas.getMetadata().add(new Metadata("exchangeId", exchange.getExchangeId()));

        if (exchange.getFromRouteId() != null) {
            metas.getMetadata().add(new Metadata("routeId", exchange.getFromRouteId()));
        }

        String corr = header(exchange, HDR_CORRELATION_ID);
        if (corr != null && !corr.isBlank()) {
            metas.getMetadata().add(new Metadata("correlationId", corr));
        }

        String service = header(exchange, HDR_SERVICE);
        if (service != null && !service.isBlank()) {
            metas.getMetadata().add(new Metadata("service", service));
        }

        String operation = header(exchange, HDR_OPERATION);
        if (operation != null && !operation.isBlank()) {
            metas.getMetadata().add(new Metadata("operation", operation));
        }

        metas.getMetadata().add(new Metadata("exceptionClass", ex.getClass().getName()));

        return metas;
    }

    /**
     * Construit un DOM Element pour ErrorReport.Data (xs:any).
     * Exemple:
     *
     * <details>
     *   <errorId>...</errorId>
     *   <service>...</service>
     *   <operation>...</operation>
     * </details>
     */
    public static Element buildErrorReportAnyData(Exchange exchange, Exception ex, String errorId) {
        try {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .newDocument();

            Element details = doc.createElement("details");

            Element eId = doc.createElement("errorId");
            eId.setTextContent(errorId);
            details.appendChild(eId);

            Element exClass = doc.createElement("exceptionClass");
            exClass.setTextContent(ex.getClass().getName());
            details.appendChild(exClass);

            String service = header(exchange, HDR_SERVICE);
            if (service != null) {
                Element s = doc.createElement("service");
                s.setTextContent(service);
                details.appendChild(s);
            }

            String op = header(exchange, HDR_OPERATION);
            if (op != null) {
                Element o = doc.createElement("operation");
                o.setTextContent(op);
                details.appendChild(o);
            }

            String corr = header(exchange, HDR_CORRELATION_ID);
            if (corr != null) {
                Element c = doc.createElement("correlationId");
                c.setTextContent(corr);
                details.appendChild(c);
            }

            return details;
        } catch (Exception domEx) {
            // fallback: si DOM échoue, on retourne un élément minimal
            try {
                Document doc = DocumentBuilderFactory.newInstance()
                        .newDocumentBuilder()
                        .newDocument();
                Element details = doc.createElement("details");
                details.setTextContent("Unable to build anydata: " + domEx.getMessage());
                return details;
            } catch (Exception ignored) {
                return null;
            }
        }
    }

    /**
     * Optionnel: root <data> (anydata) si tu veux y mettre quelque chose.
     */
    public static Element buildRootAnyData(Exchange exchange, Exception ex, String errorId) {
        try {
            Document doc = DocumentBuilderFactory.newInstance()
                    .newDocumentBuilder()
                    .newDocument();

            Element payload = doc.createElement("payload");

            Element eId = doc.createElement("errorId");
            eId.setTextContent(errorId);
            payload.appendChild(eId);

            Element msg = doc.createElement("message");
            msg.setTextContent(safeMessage(ex));
            payload.appendChild(msg);

            return payload;
        } catch (Exception ignored) {
            return null;
        }
    }

    // ---------------------------------------------------------------------
    // Helpers (résolution & sécurité)
    // ---------------------------------------------------------------------

    private static String resolveProjectName(Exchange exchange) {
        // Tu peux surcharger via header si tu veux
        String v = exchange.getIn() != null ? exchange.getIn().getHeader("X-PROJECT-NAME", String.class) : null;
        return (v == null || v.isBlank()) ? DEFAULT_PROJECT_NAME : v;
    }

    private static String resolveEngineName() {
        // Hostname (si disponible)
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception ignored) {
            return DEFAULT_ENGINE_NAME;
        }
    }

    private static String resolveMsgCode(Exchange exchange, Exception ex) {
        // Exemple de mapping simple (à adapter)
        // - ValidationException -> ERR-400
        // - TimeoutException -> ERR-504
        // - sinon ERR-500
        String className = ex.getClass().getName();

        if (className.contains("Validation")) {
            return "ERR-400";
        }
        if (className.contains("Timeout")) {
            return "ERR-504";
        }
        return DEFAULT_MSG_CODE;
    }

    private static String safeMessage(Exception ex) {
        String msg = ex.getMessage();
        if (msg == null || msg.isBlank()) {
            return "Unexpected error";
        }
        // Évite les messages gigantesques en JMS
        if (msg.length() > 2000) {
            return msg.substring(0, 2000) + "...";
        }
        return msg;
    }

    private static String header(Exchange exchange, String name) {
        if (exchange == null || exchange.getIn() == null) return null;
        return exchange.getIn().getHeader(name, String.class);
    }

    private static String stackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.flush();
        return sw.toString();
    }
}
