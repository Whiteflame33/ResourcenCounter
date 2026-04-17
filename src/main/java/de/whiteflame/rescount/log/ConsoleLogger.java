package de.whiteflame.rescount.log;

import de.whiteflame.rescount.api.log.ILogger;
import de.whiteflame.rescount.api.log.LogConfig;
import de.whiteflame.rescount.api.log.LogLevel;

import java.io.PrintStream;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class ConsoleLogger implements ILogger {
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final String LOG_LEVEL_FORMAT = "%" + LogLevel.MAX_CHAR_WIDTH + "s";

    private static final PrintStream ERROR_STREAM = System.err;
    private static final PrintStream PRINT_STREAM = System.out;

    private String classPrefix = ConsoleLogger.class.getName();

    @Override
    public void info(String msg, Object... args) {
        log(LogLevel.INFO, msg, args);
    }

    @Override
    public void debug(String msg, Object... args) {
        log(LogLevel.DEBUG, msg, args);
    }

    @Override
    public void trace(String msg, Object... args) {
        log(LogLevel.TRACE, msg, args);
    }

    @Override
    public void warn(String msg, Object... args) {
        log(LogLevel.WARN, msg, args);
    }

    @Override
    public void error(String msg, Object... args) {
        log(LogLevel.ERROR, msg, args);
    }

    @Override
    public void log(LogLevel level, String msg, Object... args) {
        if (level.ordinal() >= LogConfig.GLOBAL_LOG_LEVEL.ordinal())
            localLog(level.name(), msg, args);
    }

    public void flog(String msg, Object... args) {
        localLog("LOG", msg, args);
    }

    @Override
    public void flog(LogLevel level, String msg, Object... args) {
        localLog(level.name(), msg, args);
    }

    private void localLog(String level, String msg, Object... args) {
        final var printStream = LogLevel.ERROR.name().equals(level)
                            ? ERROR_STREAM
                            : PRINT_STREAM;

        List<Object> arguments = new LinkedList<>(Arrays.asList(args));
        List<Throwable> throwList = new ArrayList<>();

        for (var a : args) {
            if (a instanceof Throwable throwable) {
                throwList.add(throwable);
                arguments.remove(a);
            }
        }

        final var message = format(msg, arguments.toArray());
        final var logLevelFormat = LOG_LEVEL_FORMAT.formatted(level);

        printStream.printf("%s [%s] [%s] %s%n",
                DATE_FORMAT.format(new Date()), logLevelFormat, classPrefix, message);

        logThrowables(throwList);
    }

    @Override
    public void setClassPrefix(String prefix) {
        this.classPrefix = prefix;
    }

    private void logThrowables(List<Throwable> throwList) {
        throwList.forEach(t -> t.printStackTrace(ERROR_STREAM));
    }

    private static String format(String msg, Object... args) {
        if (args == null || args.length == 0)
            return msg;

        return msg.replace("{}", "%s").formatted(args);
    }
}
