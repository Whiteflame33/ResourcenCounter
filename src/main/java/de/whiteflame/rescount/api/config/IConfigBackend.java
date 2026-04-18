package de.whiteflame.rescount.api.config;

import java.util.Optional;

public interface IConfigBackend {
    void load();
    void save();
    Optional<String> getValue(String key);
    void setValue(String key, String value);
}
