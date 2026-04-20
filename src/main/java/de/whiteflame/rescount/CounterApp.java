package de.whiteflame.rescount;

import de.whiteflame.rescount.api.io.FileType;
import de.whiteflame.rescount.api.log.ILogger;
import de.whiteflame.rescount.api.log.LoggerFactory;
import de.whiteflame.rescount.api.service.ICounterListener;
import de.whiteflame.rescount.api.ui.IAppUi;
import de.whiteflame.rescount.config.GlobalConfig;
import de.whiteflame.rescount.io.FileConstants;
import de.whiteflame.rescount.io.FileHandler;
import de.whiteflame.rescount.io.impl.BinaryFileImpl;
import de.whiteflame.rescount.io.impl.CompressedBinaryFileImpl;
import de.whiteflame.rescount.io.impl.TextFileImpl;
import de.whiteflame.rescount.io.impl.xml.XmlSlimFileImpl;
import de.whiteflame.rescount.io.impl.xml.XmlVerboseFileImpl;
import de.whiteflame.rescount.ui.SwingAppUiImpl;

import javax.swing.*;
import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class CounterApp {
    private static final ILogger LOGGER = LoggerFactory.getLogger(CounterApp.class);

    private final CounterService service;
    private final FileHandler fileHandler;
    private final GlobalConfig config;

    public CounterApp(GlobalConfig config) {
        this.config = config;
        this.service = new CounterService();
        this.fileHandler = new FileHandler();

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

    public void start() {
        loadData();
        IAppUi ui = new SwingAppUiImpl(service, Main.WORD_RESOURCE, this::saveData);
        service.addListener((ICounterListener) ui);
        SwingUtilities.invokeLater(ui::show);
    }

    public void loadData() {
        LOGGER.info("Trying to load existing file(s)");

        List<File> priorities = List.of(
                config.getDataFile(FileConstants.TEXT_FILE_EXT),
                config.getDataFile(FileConstants.XML_FILE_EXT),
                config.getDataFile(FileConstants.DATA_FILE_EXT),
                config.getDataFile(FileConstants.COMPRESSED_DATA_FILE_EXT)
        );

        Map<String, List<LocalDateTime>> loaded = null;

        for (var file : priorities) {
            if (file.exists()) {
                loaded = fileHandler.load(file);
                if (file != priorities.getLast()) {
                    LOGGER.info("Found file {}. Migrating...", file.getAbsolutePath());
                    LOGGER.info("Deleting file {}", file.getAbsolutePath());
                    file.deleteOnExit();
                } else
                    LOGGER.info("Found compressed binary file.");
            }
        }

        if (loaded != null) {
            LOGGER.info("Initializing loaded entries");
            service.setEntries(loaded);
        }
    }

    public void saveData() {
        if (!service.hasChangedFromLoaded()) {
            LOGGER.info("No changes. Abort saving...");
            return;
        }

        LOGGER.info("Trying to save to compressed binary file...");
        try {
            fileHandler.save(service.getEntries(),
                    config.getDataFile(FileConstants.COMPRESSED_DATA_FILE_EXT),
                    config.get(GlobalConfig.DATA_TYPE));
            LOGGER.info("Done saving to compressed binary file.");
        } catch (Exception e) {
            LOGGER.error("Failed to save to compressed binary file", e);
        }
    }
}
