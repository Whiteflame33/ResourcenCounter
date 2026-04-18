package de.whiteflame.rescount.config;

import de.whiteflame.rescount.api.config.IConfigBackend;
import de.whiteflame.rescount.api.config.IConfigParser;
import de.whiteflame.rescount.api.log.LogConfig;
import de.whiteflame.rescount.api.log.LogLevel;
import de.whiteflame.rescount.util.DeviceIdentifier;

import java.util.Optional;

public class GlobalConfig {
    public static final String
            KEY_LOG_LEVEL = "log.level",
            KEY_DEVICE_ID = "device.id";

    private static IConfigBackend backend;
    private static IConfigParser parser;

    public static void init(IConfigBackend backend, IConfigParser parser) {
        GlobalConfig.backend = backend;
        GlobalConfig.parser = parser;
        GlobalConfig.backend.load();

        if (getDeviceId() == null) {
            setDeviceId(DeviceIdentifier.generateDeviceKey());
        }
        if (backend.getValue(KEY_LOG_LEVEL).isEmpty()) {
            set(KEY_LOG_LEVEL, LogLevel.INFO);
            backend.save();
        }
    }

    public static String getDeviceId() {
        return backend.getValue(KEY_DEVICE_ID).orElse(null);
    }

    public static void setDeviceId(String deviceId) {
        backend.setValue(KEY_DEVICE_ID, deviceId);
        backend.save();
    }

    public static void updateRuntimeLogLevel(LogLevel level) {
        if (level == null)
            return;

        LogConfig.GLOBAL_LOG_LEVEL = level;
        set(KEY_LOG_LEVEL, level.name());
        backend.save();
    }

    public static <T> void set(String key, T value) {
        backend.setValue(key, String.valueOf(value));
    }

    public static <T> T get(String key, Class<T> type, T defaultValue) {
        return Optional.ofNullable(get(key, type)).orElse(defaultValue);
    }

    public static <T> T get(String key, Class<T> type) {
        return backend.getValue(key)
                .map(val -> parser.parse(val, type))
                .orElse(null);
    }
}
