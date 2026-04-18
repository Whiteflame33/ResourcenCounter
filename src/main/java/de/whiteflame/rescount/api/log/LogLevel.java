package de.whiteflame.rescount.api.log;

public enum LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
    FORCE;

    public static final int MAX_CHAR_WIDTH = ERROR.name().length();

    public static LogLevel from(String value) {
        for (LogLevel level : LogLevel.values()) {
            if (level.name().equalsIgnoreCase(value)) {
                return level;
            }
        }
        return null;
    }
}
