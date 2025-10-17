package emailreader.utilis;

public class EmailCleaner {

    public static String extractEmailAddress(String from) {
        if (from.contains("<") && from.contains(">")) {
            int start = from.indexOf("<") + 1;
            int end = from.indexOf(">");
            return from.substring(start, end).trim();
        }
        return from.trim();
    }

    public static String extractDomain(String email) {
        int atIndex = email.indexOf('@');
        return atIndex > 0 ? email.substring(atIndex + 1) : "";
    }
}
