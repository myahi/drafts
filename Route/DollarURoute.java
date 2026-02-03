package fr.labanquepostale.marches.eai.core.route;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.exec.ExecBinding;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import fr.labanquepostale.marches.eai.core.dollaru.DollarUCommandRequest;
import fr.labanquepostale.marches.eai.core.helper.AuditHelper;
import fr.labanquepostale.marches.eai.core.helper.ErrorHelper;
import fr.labanquepostale.marches.eai.core.model.audit.Metadata;
import fr.labanquepostale.marches.eai.core.model.audit.Reference;

@Component
public class DollarURoute extends RouteBuilder {
	@Autowired
	private AuditHelper auditHelper;
	
	@Autowired
	private ErrorHelper errorHelper;

    private static final String H_EXEC_EXIT   = "CamelExecExitValue";
    private static final String H_EXEC_STDERR = "CamelExecStderr";

    @Override
    public void configure() {

        // Catch-all        
        onException(Exception.class)
        .handled(true)
        .process(this::handleException);

        from("direct:dollarUCommand")
            .routeId("dollarUCommandRoute")

            .process(exchange -> {
                DollarUCommandRequest req = exchange.getMessage().getBody(DollarUCommandRequest.class);
                if (req == null || req.getCommandParameters() == null) {
                    throw new IllegalArgumentException("Missing commandParameters");
                }
                if (req.getCommandParameters().getFlowType() == null
                        || req.getCommandParameters().getFlowType().isBlank()) {
                    throw new IllegalArgumentException("Missing commandParameters.flowType");
                }
            })

            .process(exchange -> {
                DollarUCommandRequest req = exchange.getMessage().getBody(DollarUCommandRequest.class);
                int quantity = req.getCommandParameters().getQuantity();
                exchange.setProperty("auditStatus", (quantity > 0) ? "ERROR" : "SUCCESS");
            })

            // --- Construire la commande ---
            .process(exchange -> {
                DollarUCommandRequest req = exchange.getMessage().getBody(DollarUCommandRequest.class);

                String script = exchange.getContext().resolvePropertyPlaceholders("{{dollarU.script}}");

                String flowType = req.getCommandParameters().getFlowType();
                int quantity = req.getCommandParameters().getQuantity();
                String dollarUData = req.getCommandParameters().getDollarUData();
                // exécutable
                exchange.setProperty("execScript", script);
                // arguments
                List<String> args = new ArrayList<>();
                args.add(flowType);
                args.add(String.valueOf(quantity));
                if (dollarUData != null && !dollarUData.isBlank()) {
                    args.add(dollarUData);
                }
                exchange.setProperty("execArgs", args);
            })

            // --- Exécuter seulement si dollarU.enabled=true ---
            .choice()
                .when(simple("{{dollarU.enabled}} == 'false'"))
                    .log("DollarU disabled – skipping exec")
                    .setHeader(H_EXEC_EXIT).constant(0)
                .endChoice()
                .otherwise()
                	.log("DollarU exec: ${exchangeProperty.execScript} ${exchangeProperty.execArgs}")
                    .setHeader(ExecBinding.EXEC_COMMAND_EXECUTABLE,exchangeProperty("execScript"))
                    .setHeader(ExecBinding.EXEC_COMMAND_ARGS,exchangeProperty("execArgs"))
                    .to("exec:dummy?useStderrOnEmptyStdout=true")
            .end()
            .choice()
                .when(simple("${header." + H_EXEC_EXIT + "} != 0"))
                    .setProperty("execStderr", header(H_EXEC_STDERR))
                    .throwException(new RuntimeException(
                        "DollarU command failed - exitCode=${header.CamelExecExitValue}"
                    ))
                 .otherwise().process(exchange->{
                	 
                 })
            .end()
            .delay(simple("{{dollarU.pause.ms}}"))
            
            .process(exchange->{
            
            DollarUCommandRequest dollarURequest = exchange.getMessage().getBody(DollarUCommandRequest.class);
           	 auditHelper.audit(exchange)
                .desc("Valorisation de la Ressource Logique DollarU")
                .status(String.valueOf(exchange.getProperty("auditStatus")))
                .data(dollarURequest.getCommandParameters().getFlowType())
                .references(dollarURequest.getReferences())
                .ref(Reference.class.cast(exchange.getProperty("auditReference")).getCodifier(), Reference.class.cast(exchange.getProperty("auditReference")).getCode())
                .meta("PROCESS_NAME", this.getClass().getName())
                .send();
   	        })
            // --- Trace ---
            .log("DollarU status=${exchangeProperty.auditStatus}, exitCode=${header.CamelExecExitValue}");
    }
    private void handleException(Exchange exchange) {
    	Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
		DollarUCommandRequest dollarURequest = exchange.getMessage().getBody(DollarUCommandRequest.class);
		Map<String, Object> metadatas = new HashMap<>();
		for (Metadata metadata : dollarURequest.getMetadatas()) {
    		metadatas.put(metadata.getKey(),metadata.getValue());
		}
		errorHelper.error(exchange, exception)
		         .references(dollarURequest.getReferences())
				.metas(metadatas)
				.send();
		
	}
}
