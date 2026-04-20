package de.whiteflame.rescount;

import de.whiteflame.rescount.api.log.LogConfig;
import de.whiteflame.rescount.config.GlobalConfig;
import de.whiteflame.rescount.api.log.ILogger;
import de.whiteflame.rescount.api.log.LoggerFactory;
import de.whiteflame.rescount.config.impl.XmlConfigBackend;
import de.whiteflame.rescount.config.impl.XmlConfigParser;
import de.whiteflame.rescount.log.ConsoleLogger;

import java.io.*;
import java.util.*;
import javax.swing.*;

public class Main {
    public static final File CONFIG_FILE = new File("config.xml");
    public static final String WORD_RESOURCE = "Räsorße";

    private static final ILogger LOGGER;

    static GlobalConfig config;

    static {
        LoggerFactory.setLoggerInstance(ConsoleLogger.class);
        LOGGER = LoggerFactory.getLogger(Main.class);
    }

    static void main() {
        config = new GlobalConfig(new XmlConfigBackend(CONFIG_FILE), new XmlConfigParser());

        LogConfig.GLOBAL_LOG_LEVEL = config.get(GlobalConfig.LOG_LEVEL);
        LOGGER.info("Starting app with DeviceID {}", config.get(GlobalConfig.DEVICE_ID));

        var app = new CounterApp(config);
        app.start();
    }
}

