import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.exec.ExecBinding;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class DollarURoute extends RouteBuilder {

    private static final String H_EXEC_EXIT   = "CamelExecExitValue";
    private static final String H_EXEC_STDERR = "CamelExecStderr";

    @Override
    public void configure() {

        // Catch-all
        onException(Exception.class)
            .handled(true)
            .setProperty("errorMessage", simple("${exception.message}"))
            .to("direct:handleError");

        from("direct:dollarUCommand")
            .routeId("dollarUCommandRoute")

            // --- Validation minimale ---
            .process(e -> {
                DollarUCommandRequest req = e.getMessage().getBody(DollarUCommandRequest.class);
                if (req == null || req.getCommandParameters() == null) {
                    throw new IllegalArgumentException("Missing commandParameters");
                }
                if (req.getCommandParameters().getFlowType() == null
                        || req.getCommandParameters().getFlowType().isBlank()) {
                    throw new IllegalArgumentException("Missing commandParameters.flowType");
                }
            })

            // --- Statut selon règle: quantity > 0 => ERROR sinon SUCCESS ---
            .process(e -> {
                DollarUCommandRequest req = e.getMessage().getBody(DollarUCommandRequest.class);
                int quantity = req.getCommandParameters().getQuantity();
                e.setProperty("auditStatus", (quantity > 0) ? "ERROR" : "SUCCESS");
            })

            // --- Construire la commande ---
            .process(e -> {
                DollarUCommandRequest req = e.getMessage().getBody(DollarUCommandRequest.class);

                String script = e.getContext()
                        .resolvePropertyPlaceholders("{{dollarU.script}}");

                String flowType = req.getCommandParameters().getFlowType();
                int quantity = req.getCommandParameters().getQuantity();
                String dollarUData = req.getCommandParameters().getDollarUData();

                // exécutable
                e.setProperty("execScript", script);

                // arguments
                List<String> args = new ArrayList<>();
                args.add(flowType);
                args.add(String.valueOf(quantity));
                if (dollarUData != null && !dollarUData.isBlank()) {
                    args.add(dollarUData);
                }
                e.setProperty("execArgs", args);
            })

            // --- Exécuter seulement si dollarU.enabled=true ---
            .choice()
                .when(simple("{{dollarU.enabled}} == 'false'"))
                    .log("DollarU disabled – skipping exec")
                    // simule un succès d'exec
                    .setHeader(H_EXEC_EXIT).constant(0)
                .otherwise()
                    // contrat Camel 4.x
                    .setHeader(ExecBinding.EXEC_COMMAND_EXECUTABLE,
                               exchangeProperty("execScript"))
                    .setHeader(ExecBinding.EXEC_COMMAND_ARGS,
                               exchangeProperty("execArgs"))
                    // dummy requis
                    .to("exec:dummy?useStderrOnEmptyStdout=true")
            .end()

            // --- Si exitCode != 0 => erreur ---
            .choice()
                .when(simple("${header." + H_EXEC_EXIT + "} != 0"))
                    .setProperty("execStderr", header(H_EXEC_STDERR))
                    .throwException(new RuntimeException(
                        "DollarU command failed - exitCode=${header.CamelExecExitValue}"
                    ))
            .end()

            // --- Sleep / cooldown ---
            .delay(simple("{{dollarU.pause.ms}}"))

            // --- Audit ---
            .setHeader("auditStatus", exchangeProperty("auditStatus"))
            .to("direct:audit")

            // --- Trace ---
            .log("DollarU status=${exchangeProperty.auditStatus}, exitCode=${header.CamelExecExitValue}");
    }
}
