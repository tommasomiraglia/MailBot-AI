package emailreader;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.stereotype.Service;
import emailreader.config.AiProperties;
import emailreader.config.EmailProperties;
import emailreader.service.EmailFilterService;
import emailreader.service.OpenAIResponder;
import emailreader.utilis.emailCleaner;

@Service
public class RouteApp extends RouteBuilder {
    private final EmailProperties emailConfig;
    private final OpenAIResponder openAIResponder;
    private final EmailFilterService emailFilterService;
    private final static String TOKEN_ESCALATION = "[HLN_ESCALATION]";

    public RouteApp(final EmailProperties emailConfig, final AiProperties aiProperties) throws Exception {
        this.emailConfig = emailConfig;
        this.openAIResponder = new OpenAIResponder(aiProperties.getKey(), aiProperties.getModel());
        Set<String> allowedSenders;
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("whitelist.txt")) {
            if (is == null) {
                throw new IllegalStateException("File whitelist.txt non trovato in resources");
            }
            allowedSenders = new BufferedReader(new InputStreamReader(is))
                    .lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
        }
        this.emailFilterService = new EmailFilterService(emailConfig, allowedSenders);
    }

    @Override
    public void configure() {
String imapUri = "imap://" + emailConfig.getImapHost()
        + "?username=" + emailConfig.getUsername()
        + "&password=" + emailConfig.getPassword()
        + "&unseen=true";


        String smtpUri = "smtp://" + emailConfig.getSmtpHost()
                + "?username=" + emailConfig.getUsername()
                + "&password=" + emailConfig.getPassword()
                + "&mail.smtp.auth=true"
                + "&mail.smtp.starttls.enable=true"
                + "&mail.smtp.ssl.trust=*";

        from(imapUri)
                .routeId("OpenAIEmailResponder")
                .log("New email received. Subject: ${header.subject}, Sender: ${header.from}")
                .setHeader("OriginalFrom", simple("${header.from}"))
                .setHeader("OriginalSubject", simple("${header.subject}"))
                .setHeader("OriginalBody", body())
                .choice()
                .when(exchange -> emailFilterService
                        .isAllowedSender(exchange.getIn().getHeader("OriginalFrom", String.class)))
                .log("Email from allowed sender: ${header.OriginalFrom} - Processing")
                .bean(openAIResponder, "generateResponse").id("call-ai-responder")
                .setProperty("aiResponse", body())
                .removeHeaders("Camel*", "Content-Type", "Date", "Message-ID", "Mime-Version",
                        "Received", "Return-Path", "X-Original-To", "X-Mailer", "Original*")
                .process(this::handleAiResponse)
                .choice()
                .when(simple("${exchangeProperty.IsEscalated} == true"))
                .to("direct:handleEscalationAndNotifyCustomer")
                .otherwise()
                .to("direct:handleStandardReply")
                .endChoice()
                .otherwise()
                .log("Email from unauthorized sender: ${header.OriginalFrom} - Ignored")
                .end();

        from("direct:handleEscalationAndNotifyCustomer")
                .routeId("EscalationAndCustomerNotifier")
                .log("Sending escalation email to expert: ${header.To}")
                .to(smtpUri)
                .log("Escalation email sent to: ${header.To}")
                .process(this::prepareCustomerNotification)
                .log("Sending notification email to customer: ${header.To}")
                .to(smtpUri)
                .log("Customer notification sent to: ${header.To}");

        from("direct:handleStandardReply")
                .routeId("StandardReplyHandler")
                .log("Sending standard AI reply to customer: ${header.To}")
                .to(smtpUri)
                .log("Standard AI reply sent to: ${header.To}");
    }

    private void handleAiResponse(final Exchange exchange) {
        final String aiResponse = exchange.getProperty("aiResponse", String.class);
        final String originalFrom = exchange.getIn().getHeader("OriginalFrom", String.class);
        final String originalSubject = exchange.getIn().getHeader("OriginalSubject", String.class);
        final String originalBody = exchange.getIn().getHeader("OriginalBody", String.class);

        final String senderEmail = emailConfig.getUsername();
        final String recipient = (originalFrom != null && !originalFrom.contains("@localhost"))
                ? originalFrom
                : senderEmail;

        exchange.setProperty("originalRecipient", recipient);
        exchange.getIn().setHeader("From", senderEmail);
        exchange.getIn().setHeader("Content-Type", "text/plain");

        if (aiResponse != null && aiResponse.contains(TOKEN_ESCALATION)) {
            processEscalation(exchange, aiResponse, originalSubject, originalBody, recipient);
        } else {
            processStandardReply(exchange, aiResponse, originalSubject, recipient);
        }
    }

    private void processEscalation(final Exchange exchange, final String aiResponse,
            final String originalSubject, final String originalBody, final String recipient) {
        exchange.setProperty("IsEscalated", true);
        final String cleanResponse = aiResponse.replace(TOKEN_ESCALATION, "").trim();
        exchange.setProperty("cleanAiResponse", cleanResponse);

        final String escalationSubject = "Escalation: " + originalSubject;
        final String escalationBody = "Il SupportBot ha identificato un problema complesso.\n\n"
                + "--- Dettagli Cliente ---\n"
                + "Mittente originale: " + recipient + "\n"
                + "Oggetto: " + originalSubject + "\n\n"
                + "--- Messaggio originale ---\n"
                + originalBody + "\n\n"
                + "--- Risposta AI ---\n"
                + cleanResponse + "\n\n"
                + "Si prega di prendere in carico la richiesta.";

        log.info("Email in escalation. Destinatario esperto: {}, Oggetto: {}", emailConfig.getExpertEmail(),
                escalationSubject);
        exchange.getIn().setHeader("To", emailConfig.getExpertEmail());
        exchange.getIn().setHeader("Subject", escalationSubject);
        exchange.getIn().setBody(escalationBody);
    }

    /** Prepara la risposta standard al cliente */
    private void processStandardReply(final Exchange exchange, final String aiResponse,
            final String originalSubject, final String recipient) {
        exchange.setProperty("IsEscalated", false);
        exchange.getIn().setHeader("To", emailCleaner.extractEmailAddress(recipient));
        System.out.println(emailCleaner.extractEmailAddress(recipient));
        exchange.getIn().setHeader("Subject", "Re: " + originalSubject);
        exchange.getIn().setBody(aiResponse);
    }

    /** Dopo invio escalation, prepara notifica per il cliente */
    private void prepareCustomerNotification(final Exchange exchange) {
        final String recipient = exchange.getProperty("originalRecipient", String.class);
        final String originalSubject = exchange.getIn().getHeader("OriginalSubject", String.class);
        final String cleanAiResponse = exchange.getProperty("cleanAiResponse", String.class);
        exchange.getIn().removeHeaders("To", "Subject");
        exchange.getIn().setHeader("To", emailCleaner.extractEmailAddress(recipient));
        exchange.getIn().setHeader("Subject", "Re: " + originalSubject);
        exchange.getIn().setBody(cleanAiResponse);
    }
}
