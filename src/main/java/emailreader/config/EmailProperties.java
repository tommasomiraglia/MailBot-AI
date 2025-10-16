package emailreader.config;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "email") 
public class EmailProperties {
    private String imapHost;
    private String smtpHost;
    private String username;
    private String password;
    private String expertEmail;
    private List<String> allowedSenders = new ArrayList<>();

    public String getImapHost() {
        return imapHost;
    }

    public void setImapHost(String imapHost) {
        this.imapHost = imapHost;
    }

    public String getSmtpHost() {
        return smtpHost;
    }

    public void setSmtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getExpertEmail() {
        return expertEmail;
    }

    public void setExpertEmail(String expertEmail) {
        this.expertEmail = expertEmail;
    }

    public List<String> getAllowedSenders() {
        return allowedSenders;
    }

    public void setAllowedSenders(List<String> allowedSenders) {
        this.allowedSenders = allowedSenders;
    }
}