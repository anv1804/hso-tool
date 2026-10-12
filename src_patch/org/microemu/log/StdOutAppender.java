package org.microemu.log;

public class StdOutAppender implements LoggerAppender {
    public static boolean enabled = false;

    public StdOutAppender() {
    }

    public static String formatLocation(StackTraceElement element) {
        return "";
    }

    public void append(LoggingEvent event) {
        // Silenced to eliminate console flooding and UI stuttering
    }
}
