package shared.utils;

import java.time.Instant;

public class FormatUtils {
    public static String formatTime(Instant instant) {
        if (instant == null) {
            return "Unknown";
        }
        java.time.LocalDateTime dateTime = java.time.LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
        java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return dateTime.format(formatter);
    }
}