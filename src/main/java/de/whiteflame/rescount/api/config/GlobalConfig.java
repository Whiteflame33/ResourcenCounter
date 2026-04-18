package de.whiteflame.rescount.api.config;

import java.util.UUID;

public final class GlobalConfig {
    private static GlobalConfig INSTANCE = new GlobalConfig(UUID.randomUUID().toString());

    public final String CLIENT_ID;

    private GlobalConfig(String clientId) {
        this.CLIENT_ID = clientId;
    }

    public static void init(String clientId) {
        INSTANCE = new GlobalConfig(clientId);
    }

    public static GlobalConfig instance() {
        return INSTANCE;
    }
}
