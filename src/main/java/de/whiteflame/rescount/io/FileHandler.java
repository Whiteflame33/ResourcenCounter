package de.whiteflame.rescount.io;

import de.whiteflame.rescount.api.io.FileType;
import de.whiteflame.rescount.api.io.IFileReader;
import de.whiteflame.rescount.api.io.IFileWriter;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class FileHandler {
    private final Map<FileType, IFileReader> fileReaders;
    private final Map<FileType, IFileWriter> fileWriters;

    public FileHandler() {
        fileReaders = new HashMap<>();
        fileWriters = new HashMap<>();
    }

    public void registerReader(FileType fileType, IFileReader reader) {
        if (fileReaders.containsKey(fileType))
            return;
        fileReaders.put(fileType, reader);
    }

    public void registerWriter(FileType fileType, IFileWriter writer) {
        if (fileWriters.containsKey(fileType))
            return;
        fileWriters.put(fileType, writer);
    }

    public Map<String, List<LocalDateTime>> load(File file) {
        System.out.println("Detecting type");
        FileType type = detectFileType(file);
        System.out.println("Detected type " + type);

        if (type == FileType.UNKNOWN)
            return null;

        var reader = fileReaders.get(type);
        if (reader == null)
            return null;

        return reader.readFile(file);
    }

    public void save(Map<String, List<LocalDateTime>> mapping, File file, FileType fileType) {
        if (fileType == FileType.UNKNOWN)
            fileType = FileType.TEXT;

        try {
            if (!file.exists())
                file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        fileWriters.get(fileType).writeFile(file, mapping);
    }

    private FileType detectFileType(File file) {
        for (var reader : fileReaders.values()) {
            System.out.println("Checking if " + reader.getClass().getSimpleName() + " can load file");
            if (reader.isType(file))
                return reader.getFileType();
        }

        return FileType.UNKNOWN;
    }
}
