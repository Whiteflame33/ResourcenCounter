package de.whiteflame.rescount.api.log;

public enum LogLevel {
    TRACE,
    DEBUG,
    INFO,
    WARN,
    ERROR,
    FORCE;

    public static final int MAX_CHAR_WIDTH = ERROR.name().length();
}
