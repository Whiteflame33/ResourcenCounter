package de.whiteflame.rescount.api.log;

public interface ILogger {
    void info(String msg, Object... args);
    void debug(String msg, Object... args);
    void trace(String msg, Object... args);
    void warn(String msg, Object... args);
    void error(String msg, Object... args);
    void log(LogLevel level, String msg, Object... args);
    void flog(LogLevel level, String msg, Object... args);
    void setClassPrefix(String prefix);
}
