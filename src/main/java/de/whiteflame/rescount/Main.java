package de.whiteflame.rescount;

import de.whiteflame.rescount.api.log.LogConfig;
import de.whiteflame.rescount.api.log.LogLevel;
import de.whiteflame.rescount.config.GlobalConfig;
import de.whiteflame.rescount.api.io.FileType;
import de.whiteflame.rescount.api.log.ILogger;
import de.whiteflame.rescount.api.log.LoggerFactory;
import de.whiteflame.rescount.config.impl.XmlConfigBackend;
import de.whiteflame.rescount.config.impl.XmlConfigParser;
import de.whiteflame.rescount.io.FileConstants;
import de.whiteflame.rescount.io.FileHandler;
import de.whiteflame.rescount.io.impl.BinaryFileImpl;
import de.whiteflame.rescount.io.impl.CompressedBinaryFileImpl;
import de.whiteflame.rescount.io.impl.TextFileImpl;
import de.whiteflame.rescount.io.impl.xml.XmlSlimFileImpl;
import de.whiteflame.rescount.io.impl.xml.XmlVerboseFileImpl;
import de.whiteflame.rescount.log.ConsoleLogger;

import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import javax.swing.*;

public class Main {
    public static final File CONFIG_FILE = new File("config.xml");
    public static final String WORD_RESOURCE = "Räsorße";

    private static final ILogger LOGGER;

    static int counter = 0;
    static Map<String, List<LocalDateTime>> entries = new LinkedHashMap<>();

    static FileHandler fileHandler;

    static {
        LoggerFactory.setLoggerInstance(ConsoleLogger.class);
        LOGGER = LoggerFactory.getLogger(Main.class);

        GlobalConfig.init(new XmlConfigBackend(CONFIG_FILE), new XmlConfigParser());

        LogConfig.GLOBAL_LOG_LEVEL = GlobalConfig.get(GlobalConfig.KEY_LOG_LEVEL, LogLevel.class, LogLevel.INFO);

        LOGGER.info("Starting app with DeviceID {}", GlobalConfig.getDeviceId());

        fileHandler = new FileHandler();

        fileHandler.registerReader(FileType.TEXT, new TextFileImpl());
        fileHandler.registerReader(FileType.XML_VERBOSE, new XmlVerboseFileImpl());
        fileHandler.registerReader(FileType.XML_SLIM, new XmlSlimFileImpl());
        fileHandler.registerReader(FileType.BYTE_1, new BinaryFileImpl());
        fileHandler.registerReader(FileType.BYTE_2, new CompressedBinaryFileImpl());

        fileHandler.registerWriter(FileType.TEXT, new TextFileImpl());
        fileHandler.registerWriter(FileType.XML_VERBOSE, new XmlVerboseFileImpl());
        fileHandler.registerWriter(FileType.XML_SLIM, new XmlSlimFileImpl());
        fileHandler.registerWriter(FileType.BYTE_1, new BinaryFileImpl());
        fileHandler.registerWriter(FileType.BYTE_2, new CompressedBinaryFileImpl());
    }

    static void main() {
        load();

        counter = entries.getOrDefault(WORD_RESOURCE, List.of()).size();

        JFrame frame = new JFrame();
        frame.setSize(300, 80);

        JPanel p = new JPanel();
        frame.add(p);

        p.add(new JLabel(WORD_RESOURCE + ":"));

        JLabel disp = new JLabel(String.valueOf(counter));
        p.add(disp);

        JButton a = new JButton("+1");
        p.add(a);

        a.addActionListener(e -> {
            ++counter;

            entries
                    .computeIfAbsent(WORD_RESOURCE, k -> new ArrayList<>())
                    .add(LocalDateTime.now());

            disp.setText(String.valueOf(counter));
        });

        frame.setLocationRelativeTo(null);
        frame.setVisible(true);

        Runtime.getRuntime().addShutdownHook(new Thread(Main::safe));

        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
    }

    static void load() {
        LOGGER.info("Trying to load existing file");

        Map<String, List<LocalDateTime>> loaded = null;

        if (FileConstants.getTextFile().exists()) {
            LOGGER.info("Found text file. Loading...");
            loaded = fileHandler.load(FileConstants.getTextFile());
            LOGGER.info("Deleting text file. Migrating...");
            FileConstants.getTextFile().deleteOnExit();
        } else if (FileConstants.getXmlFile().exists()) {
            LOGGER.info("Found xml file. Loading...");
            loaded = fileHandler.load(FileConstants.getXmlFile());
            LOGGER.info("Deleting xml file. Migrating...");
            FileConstants.getXmlFile().deleteOnExit();
        } else if (FileConstants.getDataFile().exists()) {
            LOGGER.info("Found simple binary file. Loading...");
            loaded = fileHandler.load(FileConstants.getDataFile());
            LOGGER.info("Deleting simple binary file. Migrating...");
            FileConstants.getDataFile().deleteOnExit();
        } else if (FileConstants.getCompressedDataFile().exists()) {
            LOGGER.info("Found compressed binary file. Loading...");
            loaded = fileHandler.load(FileConstants.getCompressedDataFile());
        }

        if (loaded != null) {
            LOGGER.info("Initializing loaded entries");
            entries.clear();
            entries.putAll(loaded);
        }
    }

    static void safe() {
        LOGGER.info("Trying to save to compressed binary file...");
        try {
            fileHandler.save(entries, FileConstants.getCompressedDataFile(), FileType.BYTE_2);
            LOGGER.info("Done saving to compressed binary file.");
        } catch (Exception e) {
            LOGGER.error("Failed to save to compressed binary file", e);
        }
    }
}

