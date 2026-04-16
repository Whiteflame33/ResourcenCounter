package de.whiteflame.rescount.io.impl;

import de.whiteflame.rescount.Main;
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
import java.io.Writer;
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
            List<String> lines = r.lines().toList();

            if (lines.isEmpty())
                return map;

            int count = Integer.parseInt(lines.getFirst().split(TEXT_COUNT_SEPARATOR)[1]);

            List<LocalDateTime> list = new ArrayList<>(count);
            for (int i = 1; i < lines.size(); i++) {
                var line = lines.get(i);

                if (line == null || line.isBlank())
                    continue;

                list.add(LocalDateTime.parse(line));
            }

            map.put(Main.WORD_RESOURCE, list);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return map;
    }
}
