package de.whiteflame.rescount.io;

import de.whiteflame.rescount.api.io.FileType;
import de.whiteflame.rescount.api.io.IFileReader;
import de.whiteflame.rescount.api.io.IFileWriter;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
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
        FileType type = detectFileType(file);

        if (type == FileType.UNKNOWN)
            return null;

        return fileReaders.get(type).readFile(file);
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
        if (file.getName().endsWith(FileType.TEXT.getFileExtension()))
            return FileType.TEXT;

        try {
            var builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            var doc = builder.parse(file);

            if (doc.getDocumentElement().getTagName().equals("file"))
                return FileType.XML_VERBOSE;
            else if (doc.getDocumentElement().getTagName().equals("f"))
                return FileType.XML_SLIM;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return FileType.UNKNOWN;
    }
}
