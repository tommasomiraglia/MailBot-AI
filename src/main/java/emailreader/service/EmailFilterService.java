package emailreader.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import emailreader.config.EmailProperties;
import java.util.Set;
import java.util.HashSet;

@Service
public class EmailFilterService {
    
    private static final Logger log = LoggerFactory.getLogger(EmailFilterService.class);
    private final Set<String> allowedSenders;

    public EmailFilterService(EmailProperties emailProperties) {
        this.allowedSenders = new HashSet<>();
        for (String email : emailProperties.getAllowedSenders()) {
            allowedSenders.add(email.toLowerCase().trim());
        }
        log.info("EmailFilterService initialized with {} allowed senders", allowedSenders.size());
        if (log.isDebugEnabled()) {
            log.debug("Allowed senders: {}", allowedSenders);
        }
    }

    /**
     * Verifica se il mittente è autorizzato
     * @param from L'header "From" dell'email (può essere "Nome <email@domain.com>")
     * @return true se il mittente è autorizzato, false altrimenti
     */
    public boolean isAllowedSender(String from) {
        if (from == null || from.isEmpty()) {
            log.warn("Received null or empty 'from' address");
            return false;
        }
        String emailAddress = extractEmailAddress(from).toLowerCase();
        boolean isAllowed = allowedSenders.contains(emailAddress);
        log.info("Sender check: {} -> {}", emailAddress, isAllowed ? "ALLOWED" : "BLOCKED");
        return isAllowed;
    }

    /**
     * Estrae l'indirizzo email da una stringa tipo "Nome <email@domain.com>"
     */
    private String extractEmailAddress(String from) {
        if (from.contains("<") && from.contains(">")) {
            int start = from.indexOf("<") + 1;
            int end = from.indexOf(">");
            return from.substring(start, end).trim();
        }
        return from.trim();
    }
}