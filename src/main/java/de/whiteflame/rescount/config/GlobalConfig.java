package de.whiteflame.rescount.config;

import de.whiteflame.rescount.api.config.IConfigBackend;
import de.whiteflame.rescount.api.config.IConfigParser;
import de.whiteflame.rescount.api.io.FileType;
import de.whiteflame.rescount.api.log.LogLevel;
import de.whiteflame.rescount.util.DeviceIdentifier;

import java.io.File;

public class GlobalConfig {
    public static final ConfigKey<LogLevel> LOG_LEVEL =
            new ConfigKey<>("log.level", LogLevel.class, LogLevel.INFO);
    public static final ConfigKey<File> DATA_PATH =
            new ConfigKey<>("device.data.path", File.class,
                    new File(System.getProperty("user.home"), ".whiteflame_rescount"));
    public static final ConfigKey<String> DATA_NAME =
            new ConfigKey<>("device.data.name", String.class, "wissmann");
    public static final ConfigKey<FileType> DATA_TYPE =
            new ConfigKey<>("device.data.type", FileType.class, FileType.BYTE_2);
    public static final ConfigKey<String> DEVICE_ID =
            new ConfigKey<>("device.id", String.class, null);

    private final IConfigBackend backend;
    private final IConfigParser parser;

    public GlobalConfig(IConfigBackend backend, IConfigParser parser) {
        this.backend = backend;
        this.parser = parser;
        this.backend.load();
        ensureDefaults();
    }

    public File getDataFile(String fileExtension) {
        File baseDir = getAs(DATA_PATH);
        if (!baseDir.exists())
            baseDir.mkdirs();
        return new File(baseDir, getAs(DATA_NAME) + "." + fileExtension);
    }

    public <T> void set(ConfigKey<T> key, T value) {
        backend.setValue(key.key(), String.valueOf(value));
    }

    public <T> void setIfAbsent(ConfigKey<T> key) {
        if (backend.getValue(key.key()).isEmpty())
            set(key, key.defaultValue());
    }

    public <T> T getAs(ConfigKey<T> key) {
        return backend.getValue(key.key())
                .map(val -> parser.parse(val, key.type()))
                .orElseGet(() -> {
                    if (key.defaultValue() != null) {
                        set(key, key.defaultValue());
                        return key.defaultValue();
                    }
                    return null;
                });
    }

    public boolean isEmpty(String key) {
        return backend.getValue(key).isEmpty();
    }

    private void ensureDefaults() {
        setIfAbsent(DATA_PATH);
        setIfAbsent(DATA_NAME);
        setIfAbsent(DATA_TYPE);
        setIfAbsent(LOG_LEVEL);

        if (isEmpty(DEVICE_ID.key()))
            set(DEVICE_ID, DeviceIdentifier.generateDeviceKey());

        backend.save();
    }

    public record ConfigKey<T>(String key, Class<T> type, T defaultValue) {}
}
