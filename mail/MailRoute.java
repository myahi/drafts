package fr.labanquepostale.marches.eai.core.route;
import org.apache.camel.attachment.AttachmentMessage;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Component;

import fr.labanquepostale.marches.eai.core.mail.bean.MailingListService;
import fr.labanquepostale.marches.eai.core.mail.bean.MailingRecipients;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class MailRoute extends RouteBuilder {

    private final MailingListService mailingListService;

    public MailRoute(MailingListService mailingListService) {
        this.mailingListService = mailingListService;
    }

    @Override
    public void configure() {

        from("direct:MailRoute")
            .routeId("MailRoute")

            .process(exchange -> {
                // Required
                String listName = exchange.getMessage().getHeader("listName", String.class);
                if (listName == null || listName.isBlank()) {
                    throw new IllegalArgumentException("Missing header 'listName'");
                }

                String subject = exchange.getMessage().getHeader("subject", String.class);
                if (subject == null || subject.isBlank()) {
                    throw new IllegalArgumentException("Missing header 'subject'");
                }

                // Base recipients from DB
                MailingRecipients base = mailingListService.getRecipients(listName);

                Set<String> to = new LinkedHashSet<>(base.to());
                Set<String> cc = new LinkedHashSet<>(base.cc());

                // Merge optional extras from caller:
                mergeEmails(to, exchange.getMessage().getHeader("to"));
                mergeEmails(cc, exchange.getMessage().getHeader("cc"));

                if (to.isEmpty()) {
                    throw new IllegalStateException("No TO recipients after merge for listName=" + listName);
                }

                // Set Camel Mail headers (comma-separated)
                exchange.getMessage().setHeader("to", String.join(",", to));
                if (!cc.isEmpty()) {
                    exchange.getMessage().setHeader("cc", String.join(",", cc));
                }

                // Subject (Camel standard)
                exchange.getMessage().setHeader("subject", subject);

                // Attachments:
                // Nothing to do here if the caller already added them via AttachmentMessage.
                // But we can enforce that the message is an AttachmentMessage if needed:
                // (No-op if already is one)
                exchange.getMessage(AttachmentMessage.class);

                // Content-Type:
                // If caller sets header "Content-Type" (e.g. "text/html; charset=UTF-8"),
                // we keep it. Otherwise mail component defaults to plain text.
            })

            // Send SMTP (body + Content-Type + attachments pass through)
            .to("smtp://{{core.mail.smtp.host}}:{{core.mail.smtp.port}}?from={{core.mail.from}}");
            //.to("mail:smtp://{{core.mail.smtp.host}}:{{core.mail.smtp.port}}?from={{core.mail.from}}");
           // .to("mail://{{core.mail.smtp.host}}:{{core.mail.smtp.port}}?protocol=smtp&from={{core.mail.from}}");
    }

    private static void mergeEmails(Set<String> target, Object headerValue) {
		if (headerValue == null)
			return;
		if (headerValue instanceof String s) {
			for (String part : s.split("[,;]")) {
				String email = part.trim();
				if (!email.isEmpty())
					target.add(email);
			}
			return;
		}

        if (headerValue instanceof Iterable<?> it) {
            for (Object o : it) {
                if (o == null) continue;
                String email = o.toString().trim();
                if (!email.isEmpty()) target.add(email);
            }
        }
    }
}
