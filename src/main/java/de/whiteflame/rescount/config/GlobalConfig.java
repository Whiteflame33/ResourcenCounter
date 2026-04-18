package de.whiteflame.rescount.config;

import de.whiteflame.rescount.api.config.IConfigBackend;
import de.whiteflame.rescount.api.config.IConfigParser;
import de.whiteflame.rescount.api.log.LogLevel;
import de.whiteflame.rescount.util.DeviceIdentifier;

import java.io.File;
import java.util.Optional;

public class GlobalConfig {
    public static final String
            KEY_LOG_LEVEL = "log.level",
            KEY_DATA_PATH = "device.path.data",
            KEY_DEVICE_ID = "device.id";

    private static IConfigBackend backend;
    private static IConfigParser parser;

    public static void init(IConfigBackend backend, IConfigParser parser) {
        GlobalConfig.backend = backend;
        GlobalConfig.parser = parser;
        GlobalConfig.backend.load();

        boolean needsSave = false;

        if (getDeviceId() == null) {
            setDeviceId(DeviceIdentifier.generateDeviceKey());
        }

        if (backend.getValue(KEY_DATA_PATH).isEmpty()) {
            String defaultPath = System.getProperty("user.home") + File.separator + ".whiteflame_rescount";
            set(KEY_DATA_PATH, defaultPath);
            needsSave = true;
        }

        if (backend.getValue(KEY_LOG_LEVEL).isEmpty()) {
            set(KEY_LOG_LEVEL, LogLevel.INFO);
            needsSave = true;
        }

        if (needsSave)
            backend.save();
    }

    public static String getDeviceId() {
        return backend.getValue(KEY_DEVICE_ID).orElse(null);
    }

    public static void setDeviceId(String deviceId) {
        backend.setValue(KEY_DEVICE_ID, deviceId);
        backend.save();
    }

    public static File getDataFile(String fileName) {
        File baseDir = get(KEY_DATA_PATH, File.class);
        if (!baseDir.exists())
            baseDir.mkdirs();
        return new File(baseDir, fileName);
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
