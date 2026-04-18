package de.whiteflame.rescount.api.config;

public interface IConfigParser {
    <T> T parse(String rawValue, Class<T> type);
}
