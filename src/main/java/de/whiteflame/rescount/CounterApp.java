package de.whiteflame.rescount;

import de.whiteflame.rescount.api.app.IAppService;
import de.whiteflame.rescount.api.config.IConfigBackend;
import de.whiteflame.rescount.api.config.IConfigParser;
import de.whiteflame.rescount.api.log.ILogger;
import de.whiteflame.rescount.api.log.LogConfig;
import de.whiteflame.rescount.api.log.LogLevel;
import de.whiteflame.rescount.api.log.LoggerFactory;
import de.whiteflame.rescount.api.service.ICounterService;
import de.whiteflame.rescount.api.ui.IAppUi;
import de.whiteflame.rescount.app.LocalAppService;
import de.whiteflame.rescount.config.GlobalConfig;
import de.whiteflame.rescount.io.FileConstants;
import de.whiteflame.rescount.io.FileHandler;
import de.whiteflame.rescount.service.impl.CounterService;
import de.whiteflame.rescount.ui.SwingAppUiImpl;

import javax.swing.*;
import java.io.File;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class CounterApp {
    private final ILogger LOGGER = LoggerFactory.getLogger(CounterApp.class);

    private final IAppService service;
    private final ICounterService coreService;
    private final FileHandler fileHandler;
    private final GlobalConfig config;

    private CounterApp(GlobalConfig config) {
        this.config = config;
        this.coreService = new CounterService();
        this.fileHandler = new FileHandler();
        this.service = new LocalAppService(coreService, fileHandler, config);
    }

    public void start() {
        LOGGER.flog(LogLevel.INFO, "Starting App with ID {}", config.getAs(GlobalConfig.DEVICE_ID));

        loadData();
        IAppUi ui = new SwingAppUiImpl(service, Main.WORD_RESOURCE, service::shutdown);
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
            coreService.setEntries(loaded);
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private IConfigBackend backend;
        private IConfigParser parser;
        private Class<? extends ILogger> loggerClass;

        private Builder() {}

        public Builder withLogger(Class<? extends ILogger> clazz) {
            this.loggerClass = clazz;
            return this;
        }

        public Builder withConfigBackend(IConfigBackend backend) {
            this.backend = backend;
            return this;
        }

        public Builder withConfigParser(IConfigParser parser) {
            this.parser = parser;
            return this;
        }

        public CounterApp build() {
            if (loggerClass != null)
                LoggerFactory.setLoggerInstance(loggerClass);

            var log = LoggerFactory.getLogger(CounterApp.class);

            if (backend == null)
                throw new IllegalStateException("ConfigBackend must be provided!");

            if (parser == null)
                throw new IllegalStateException("ConfigParser must be provided!");

            GlobalConfig globalConfig = new GlobalConfig(backend, parser);

            LogConfig.GLOBAL_LOG_LEVEL = globalConfig.getAs(GlobalConfig.LOG_LEVEL);

            log.debug("Creating {}", CounterApp.class.getSimpleName());
            return new CounterApp(globalConfig);
        }
    }
}
