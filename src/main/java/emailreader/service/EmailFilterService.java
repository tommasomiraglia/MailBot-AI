package emailreader.service;

import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import emailreader.config.EmailProperties;
import emailreader.utilis.emailCleaner;

import java.util.Set;

@Service
public class EmailFilterService {
    
    private static final Logger log = LoggerFactory.getLogger(EmailFilterService.class);
    private final Set<String> allowedSenders;

    public EmailFilterService(final EmailProperties emailProperties, final Set<String> allowedSenders) {
        this.allowedSenders = allowedSenders;
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
        String emailAddress = emailCleaner.extractEmailAddress(from).toLowerCase();
        boolean isAllowed = allowedSenders.contains(emailAddress);
        log.info("Sender check: {} -> {}", emailAddress, isAllowed ? "ALLOWED" : "BLOCKED");
        return isAllowed;
    }
}