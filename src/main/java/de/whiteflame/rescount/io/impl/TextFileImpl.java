package de.whiteflame.rescount.io.impl;

import de.whiteflame.rescount.api.io.FileType;
import de.whiteflame.rescount.api.io.IFileReader;
import de.whiteflame.rescount.api.io.IFileWriter;
import de.whiteflame.rescount.api.log.ILogger;
import de.whiteflame.rescount.api.log.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TextFileImpl implements IFileReader, IFileWriter {
    private static final ILogger LOGGER = LoggerFactory.getLogger(TextFileImpl.class);
    private static final String TEXT_COUNT_SEPARATOR = "count=";

    @Override
    public FileType getFileType() {
        return FileType.TEXT;
    }

    @Override
    public void writeFile(File file, Map<String, List<LocalDateTime>> groupedTimestamps) {
        LOGGER.trace("Starting write operation to file: {}", file.getAbsolutePath());
        
        try (var writer = new PrintWriter(new BufferedWriter(new FileWriter(file)))) {
            for (var entry : groupedTimestamps.entrySet()) {
                writer.println(entry.getKey());

                writer.print(TEXT_COUNT_SEPARATOR);
                writer.println(entry.getValue().size());

                for (var t : entry.getValue()) {
                    writer.println(t.toString());
                }
            }
            
            LOGGER.trace("Successfully wrote {} categories to {}", groupedTimestamps.size(), file.getName());
        } catch (Exception e) {
            LOGGER.error("Failed to write grouped timestamps to file: {}", file.getAbsolutePath(), e);
        }
    }

    @Override
    public Map<String, List<LocalDateTime>> readFile(File file) {
        LOGGER.trace("Starting read operation to file: {}", file.getAbsolutePath());
        
        Map<String, List<LocalDateTime>> map = new LinkedHashMap<>();

        try (BufferedReader r = new BufferedReader(new FileReader(file))) {

            String line;
            String currentWord = null;
            List<LocalDateTime> currentList = null;
            int expectedCount = -1;

            while ((line = r.readLine()) != null) {
                line = line.trim();

                if (line.isEmpty())
                    continue;

                if (!line.startsWith(TEXT_COUNT_SEPARATOR) && !line.contains("T")) {
                    validateCount(currentWord, currentList, expectedCount);

                    currentWord = line;
                    currentList = new ArrayList<>();
                    map.put(currentWord, currentList);
                    expectedCount = -1;
                    continue;
                }

                if (line.startsWith(TEXT_COUNT_SEPARATOR)) {
                    try {
                        expectedCount = Integer.parseInt(line.substring(TEXT_COUNT_SEPARATOR.length()));
                    } catch (NumberFormatException e) {
                        LOGGER.warn("Invalid count format in file {} for word {}: {}", file.getName(), currentWord, line);
                    }
                    continue;
                }

                if (currentList == null) {
                    LOGGER.error("Format error: Found timestamp '{}' without a preceding word in file {}", line, file.getName());
                    throw new RuntimeException("Timestamp without word context: " + line);
                }

                try {
                    currentList.add(LocalDateTime.parse(line));
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse timestamp '{}' for word {} in file {}", line, currentWord, file.getName());
                }
            }

            validateCount(currentWord, currentList, expectedCount);

            LOGGER.trace("Finished reading file {}. Total categories loaded: {}", file.getName(), map.size());
        } catch (IOException e) {
            LOGGER.error("IO error while reading file: {}", file.getAbsolutePath(), e);
        }

        return map;
    }

    @Override
    public boolean isType(File file) {
        boolean match = file != null && file.getName().endsWith(getFileType().getFileExtension());
        if (match) {
            LOGGER.trace("File {} matched type {}", file.getName(), getFileType());
        }
        return match;
    }

    private void validateCount(String word, List<LocalDateTime> list, int expected) {
        if (list != null && expected != -1 && list.size() != expected) {
            final String errorMsg = "Count mismatch for '%s'. Expected %d, Actual: %d."
                    .formatted(word, expected, list.size());
            LOGGER.warn(errorMsg);
            throw new IllegalArgumentException(errorMsg);
        }
    }
}
