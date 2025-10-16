package emailreader.utilis;

public class emailCleaner {

    public static String extractEmailAddress(String from) {
        if (from.contains("<") && from.contains(">")) {
            int start = from.indexOf("<") + 1;
            int end = from.indexOf(">");
            return from.substring(start, end).trim();
        }
        return from.trim();
    }
}
