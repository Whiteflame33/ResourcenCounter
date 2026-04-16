package de.whiteflame.rescount.io.impl;

import de.whiteflame.rescount.api.io.FileType;
import de.whiteflame.rescount.api.io.IFileReader;
import de.whiteflame.rescount.api.io.IFileWriter;

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
    private static final String TEXT_COUNT_SEPARATOR = "count=";

    @Override
    public FileType getFileType() {
        return FileType.TEXT;
    }

    @Override
    public void writeFile(File file, Map<String, List<LocalDateTime>> groupedTimestamps) {
        try (var writer = new PrintWriter(new BufferedWriter(new FileWriter(file)))) {
            for (var entry : groupedTimestamps.entrySet()) {
                writer.println(entry.getKey());

                writer.print(TEXT_COUNT_SEPARATOR);
                writer.println(entry.getValue().size());

                for (var t : entry.getValue()) {
                    writer.println(t.toString());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, List<LocalDateTime>> readFile(File file) {
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
                    if (currentList != null && expectedCount != -1 && currentList.size() != expectedCount) {
                        throw new RuntimeException("Count mismatch for " + currentWord);
                    }

                    currentWord = line;
                    currentList = new ArrayList<>();
                    map.put(currentWord, currentList);
                    expectedCount = -1;
                    continue;
                }

                if (line.startsWith(TEXT_COUNT_SEPARATOR)) {
                    expectedCount = Integer.parseInt(line.substring(TEXT_COUNT_SEPARATOR.length()));
                    continue;
                }

                if (currentList == null) {
                    throw new RuntimeException("Timestamp without word context: " + line);
                }

                currentList.add(LocalDateTime.parse(line));
            }

            if (currentList != null && expectedCount != -1 &&
                    currentList.size() != expectedCount) {
                throw new RuntimeException("Count mismatch for " + currentWord);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return map;
    }
}
