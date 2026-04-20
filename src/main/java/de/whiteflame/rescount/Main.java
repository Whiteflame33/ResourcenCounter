package de.whiteflame.rescount;

import de.whiteflame.rescount.config.impl.XmlConfigBackend;
import de.whiteflame.rescount.config.impl.XmlConfigParser;
import de.whiteflame.rescount.log.ConsoleLogger;

import java.io.*;

public class Main {
    public static final File CONFIG_FILE = new File("config.xml");
    public static final String WORD_RESOURCE = "Räsorße";

    static void main() {
        CounterApp.builder()
                .withLogger(ConsoleLogger.class)
                .withConfigBackend(new XmlConfigBackend(CONFIG_FILE))
                .withConfigParser(new XmlConfigParser())
                .build()
                .start();
    }
}

