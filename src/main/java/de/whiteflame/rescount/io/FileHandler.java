package de.whiteflame.rescount.io;

import de.whiteflame.rescount.api.io.FileType;
import de.whiteflame.rescount.api.io.IFileReader;
import de.whiteflame.rescount.api.io.IFileWriter;
import de.whiteflame.rescount.api.log.ILogger;
import de.whiteflame.rescount.api.log.LogLevel;
import de.whiteflame.rescount.api.log.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

public final class FileHandler {
    private static final ILogger LOGGER = LoggerFactory.getLogger(FileHandler.class);

    private final Map<FileType, IFileReader> fileReaders;
    private final Map<FileType, IFileWriter> fileWriters;

    public FileHandler() {
        fileReaders = new HashMap<>();
        fileWriters = new HashMap<>();

        discoverServices();
    }

    public void discoverServices() {
        var resource = getClass().getClassLoader().getResource("META-INF/services/de.whiteflame.rescount.api.io.IFileReader");
        LOGGER.trace("SPI File URL: {}", resource);

        LOGGER.debug("Running {} discovery", IFileReader.class.getSimpleName());
        ServiceLoader<IFileReader> readers = ServiceLoader.load(IFileReader.class, IFileReader.class.getClassLoader());
        readers.forEach(reader -> registerReader(reader.getFileType(), reader));

        LOGGER.debug("Running {} discovery", IFileWriter.class.getSimpleName());
        ServiceLoader<IFileWriter> writers = ServiceLoader.load(IFileWriter.class, IFileWriter.class.getClassLoader());
        writers.forEach(writer -> registerWriter(writer.getFileType(), writer));

        LOGGER.trace("writers: {}", writers.stream().count());
        LOGGER.trace("readers: {}", readers.stream().count());
    }

    public void registerReader(FileType fileType, IFileReader reader) {
        if (fileReaders.containsKey(fileType)) {
            LOGGER.warn("Reader for file type {} is already registered.", fileType);
            return;
        }
        LOGGER.debug("Registering reader for file type {}", fileType);
        fileReaders.put(fileType, reader);
    }

    public void registerWriter(FileType fileType, IFileWriter writer) {
        if (fileWriters.containsKey(fileType)) {
            LOGGER.warn("Writer for file type {} is already registered.", fileType);
            return;
        }
        LOGGER.debug("Registering writer for file type {}", fileType);
        fileWriters.put(fileType, writer);
    }

    public Map<String, List<LocalDateTime>> load(File file) {
        LOGGER.info("Loading file {}", file);
        FileType type = detectFileType(file);
        LOGGER.info("Detected file type {}", type);

        if (type == FileType.UNKNOWN) {
            LOGGER.warn("File type {} is not supported.", type);
            return null;
        }

        var reader = fileReaders.get(type);
        if (reader == null) {
            LOGGER.warn("No reader configured for file type {}.", type);
            return null;
        }

        return reader.readFile(file);
    }

    public void save(Map<String, List<LocalDateTime>> mapping, File file, FileType fileType) {
        LOGGER.info("Saving file {}", file);

        if (fileType == FileType.UNKNOWN) {
            LOGGER.warn("File type {} is not supported. Defaulting to {}.", fileType, FileType.TEXT);
            fileType = FileType.TEXT;
        }

        try {
            if (!file.exists()) {
                file.createNewFile();
                LOGGER.info("Creating file {}", file);
            }
        } catch (IOException e) {
            LOGGER.error("Failed to save file {}", file, e);
            return;
        }

        var writer = fileWriters.get(fileType);
        if (writer == null) {
            LOGGER.warn("No writer configured for file type {}.", fileType);
            return;
        }

        writer.writeFile(file, mapping);
    }

    private FileType detectFileType(File file) {
        for (var reader : fileReaders.values()) {
            LOGGER.debug("Checking if {} can load file {}", reader.getClass().getSimpleName(), file);
            if (reader.isType(file)) {
                LOGGER.debug("Reader {} can load file {}", reader.getClass().getSimpleName(), file);
                return reader.getFileType();
            }
        }

        return FileType.UNKNOWN;
    }
}
