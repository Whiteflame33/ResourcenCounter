package de.whiteflame.rescount.config.impl;

import de.whiteflame.rescount.api.config.IConfigParser;
import de.whiteflame.rescount.api.log.LogLevel;

public class XmlConfigParser implements IConfigParser {
    @Override
    @SuppressWarnings("unchecked")
    public <T> T parse(String rawValue, Class<T> type) {
        if (type == Integer.class || type == int.class)
            return (T) Integer.valueOf(rawValue);
        if (type == Long.class || type == long.class)
            return (T) Long.valueOf(rawValue);
        if (type == Boolean.class || type == boolean.class)
            return (T) Boolean.valueOf(rawValue);
        if (type == Double.class || type == double.class)
            return (T) Double.valueOf(rawValue);
        if (type == Float.class || type == float.class)
            return (T) Float.valueOf(rawValue);
        if (type == String.class)
            return (T) rawValue;
        if (type == LogLevel.class)
            return (T) LogLevel.from(rawValue);

        throw new UnsupportedOperationException("Unsupported config type: " + type);
    }
}
