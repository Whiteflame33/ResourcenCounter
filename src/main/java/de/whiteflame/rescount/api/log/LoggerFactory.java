package de.whiteflame.rescount.api.log;

public final class LoggerFactory {
    private static Class<? extends ILogger> LOGGER_INSTANCE;

    private LoggerFactory() {}

    public static ILogger getLogger(Class<?> clazz) {
        final var FALLBACK = new ILogger() {
            @Override public void info(String msg, Object... args) { log(LogLevel.INFO, msg, args); }
            @Override public void debug(String msg, Object... args) { log(LogLevel.DEBUG, msg, args); }
            @Override public void trace(String msg, Object... args) { log(LogLevel.TRACE, msg, args); }
            @Override public void warn(String msg, Object... args) { log(LogLevel.WARN, msg, args); }
            @Override public void error(String msg, Object... args) { log(LogLevel.ERROR, msg, args); }

            @Override
            public void log(LogLevel level, String msg, Object... args) {
                if (level.ordinal() <= LogConfig.GLOBAL_LOG_LEVEL.ordinal())
                    return;

                System.out.printf("[%s] [%s] %s%n", level, clazz.getName(), msg.replace("{}", "%s").formatted(args));
            }

            @Override
            public void flog(LogLevel level, String msg, Object... args) {
                System.out.printf("[%s] [%s] %s%n", level, clazz.getName(), msg.replace("{}", "%s").formatted(args));
            }

            @Override public void setClassPrefix(String prefix) {}
        };

        if (LOGGER_INSTANCE == null) {
          return FALLBACK;
        }

        try {
            var instance = LOGGER_INSTANCE.getDeclaredConstructor().newInstance();
            instance.setClassPrefix(clazz.getName());
            return instance;
        } catch (Exception e) {
            FALLBACK.error("Failed to #getLogger()", e);
            return FALLBACK;
        }
    }

    public static void setLoggerInstance(Class<? extends ILogger> loggerInstance) {
        LOGGER_INSTANCE = loggerInstance;
    }
}
