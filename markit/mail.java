package fr.lbp.markit.alerting;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service d'envoi d'emails (Spring Mail).
 *
 * Dépendance Maven/Gradle requise :
 *   org.springframework.boot:spring-boot-starter-mail
 *
 * Propriétés attendues (exemples) :
 *   lbp.markit.email.alert.enabled=true
 *   lbp.markit.email.alert.from=markit-engine@domaine.fr
 *   lbp.markit.email.alert.to=ops@domaine.fr;support@domaine.fr
 *   lbp.markit.email.alert.subjectPrefix=[MARKIT]
 */
@Service
public class MarkitMailService {

    private static final Logger LOGGER = LogManager.getLogger(MarkitMailService.class);

    private final JavaMailSender mailSender;

    @Value("${lbp.markit.email.alert.enabled:true}")
    private boolean enabled;

    @Value("${lbp.markit.email.alert.from:}")
    private String from;

    @Value("${lbp.markit.email.alert.to:}")
    private String to;

    @Value("${lbp.markit.email.alert.subjectPrefix:[MARKIT]}")
    private String subjectPrefix;

    public MarkitMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    /**
     * Envoi d'un email simple texte.
     * - "to" peut contenir plusieurs adresses séparées par ';' ou ','.
     */
    public void send(String subject, String body) {
        if (!enabled) {
            LOGGER.info("Email alert disabled: skip send (subject={})", subject);
            return;
        }

        String[] recipients = parseRecipients(to);
        if (recipients.length == 0) {
            LOGGER.warn("Email alert enabled but no recipients configured (lbp.markit.email.alert.to). Skip send.");
            return;
        }

        SimpleMailMessage msg = new SimpleMailMessage();
        if (from != null && !from.trim().isEmpty()) {
            msg.setFrom(from.trim());
        }
        msg.setTo(recipients);
        msg.setSubject(buildSubject(subject));
        msg.setText(body == null ? "" : body);

        try {
            mailSender.send(msg);
            LOGGER.info("Email alert sent to {} (subject={})", Arrays.toString(recipients), msg.getSubject());
        } catch (Exception e) {
            // on ne veut pas casser le retry Markit à cause d'un SMTP KO
            LOGGER.error("Failed to send email alert (ignored). subject={}", msg.getSubject(), e);
        }
    }

    private String buildSubject(String subject) {
        String s = (subject == null ? "" : subject.trim());
        String prefix = (subjectPrefix == null ? "" : subjectPrefix.trim());
        if (prefix.isEmpty()) return s;
        if (s.isEmpty()) return prefix;
        return prefix + " " + s;
    }

    private static String[] parseRecipients(String raw) {
        if (raw == null) return new String[0];
        String cleaned = raw.trim();
        if (cleaned.isEmpty()) return new String[0];

        // accepte ";" ou "," comme séparateur
        String[] split = cleaned.replace(",", ";").split(";");
        return Arrays.stream(split)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .collect(Collectors.toList())
                .toArray(new String[0]);
    }
}
