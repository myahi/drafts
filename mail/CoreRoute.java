import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

@Component
public class CoreMailRoutes extends RouteBuilder {

    private final MailingListService mailingListService;

    public CoreMailRoutes(MailingListService mailingListService) {
        this.mailingListService = mailingListService;
    }

    @Override
    public void configure() {

        from("direct:core.sendMailByList")
            .routeId("core-send-mail-by-list")

            // Resolve TO/CC from DB mailing list name
            .process(e -> {
                String listName = e.getMessage().getHeader("listName", String.class);
                if (listName == null || listName.isBlank()) {
                    throw new IllegalArgumentException("Missing header 'listName'");
                }

                MailingRecipients r = mailingListService.getRecipients(listName);

                if (r.to() == null || r.to().isEmpty()) {
                    throw new IllegalStateException("No TO recipients for listName=" + listName);
                }

                e.getMessage().setHeader("CamelMailTo", String.join(",", r.to()));
                if (r.cc() != null && !r.cc().isEmpty()) {
                    e.getMessage().setHeader("CamelMailCc", String.join(",", r.cc()));
                }

                // Optional: ensure subject exists
                String subject = e.getMessage().getHeader("subject", String.class);
                if (subject == null || subject.isBlank()) {
                    throw new IllegalArgumentException("Missing header 'subject'");
                }

                // Map to Camel Mail subject header
                e.getMessage().setHeader("CamelMailSubject", subject);
                // Body + Content-Type + attachments are left untouched and will be sent as-is.
            })

            // Send via SMTP (body, Content-Type, attachments pass through)
            .to("smtp://{{core.mail.smtp.host}}:{{core.mail.smtp.port}}"
                + "?from={{core.mail.from}}");
    }
}
