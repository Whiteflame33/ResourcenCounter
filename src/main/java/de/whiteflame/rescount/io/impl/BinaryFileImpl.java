package de.whiteflame.rescount.io.impl;

import de.whiteflame.rescount.util.TimestampGrouper;
import de.whiteflame.rescount.api.io.FileType;
import de.whiteflame.rescount.api.io.IFileReader;
import de.whiteflame.rescount.api.io.IFileWriter;
import de.whiteflame.rescount.api.log.ILogger;
import de.whiteflame.rescount.api.log.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class BinaryFileImpl implements IFileReader, IFileWriter {
    private static final ILogger LOGGER = LoggerFactory.getLogger(BinaryFileImpl.class);
    private static final int MAGIC_BYTES = ('W' << 24) | ('T' << 16) | ('B' << 8) | 'S';

    @Override
    public FileType getFileType() {
        return FileType.BYTE_1;
    }

    @Override
    public void writeFile(File file, Map<String, List<LocalDateTime>> groupedTimestamps) {
        LOGGER.debug("Opening binary stream for writing: {}", file.getAbsolutePath());

        try (var out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            out.writeInt(MAGIC_BYTES);
            out.writeInt(groupedTimestamps.size());

            for (var entry : groupedTimestamps.entrySet()) {
                byte[] wordBytes = entry.getKey().getBytes(StandardCharsets.UTF_8);
                out.writeInt(wordBytes.length);
                out.write(wordBytes);

                var grouped = TimestampGrouper.group(entry.getValue());
                
                out.writeInt(grouped.size());

                for (var year : grouped) {
                    out.writeShort(year.year());

                    out.writeShort(year.days().size());

                    for (var day : year.days()) {
                        out.writeByte(day.day());

                        out.writeByte(day.hours().size());

                        for (var hour : day.hours()) {
                            out.writeByte(hour.hour());
                            out.writeInt(hour.times().size());

                            var it = hour.times().iterator();

                            int base = it.next();
                            out.writeInt(base);

                            int prev = base;

                            while (it.hasNext()) {
                                int current = it.next();
                                int delta = current - prev;

                                out.writeInt(delta);
                                prev = current;
                            }
                        }
                    }
                }
                LOGGER.trace("Category '{}' serialized with {} timestamps.", entry.getKey(), entry.getValue().size());
            }
            LOGGER.debug("Successfully saved binary file: {} ({} categories)", file.getName(), groupedTimestamps.size());
        } catch (Exception e) {
            LOGGER.error("Failed to write binary file: {}", file.getAbsolutePath(), e);
        }
    }

    @Override
    public Map<String, List<LocalDateTime>> readFile(File file) {
        LOGGER.debug("Reading binary file: {}", file.getAbsolutePath());

        Map<String, List<LocalDateTime>> map = new LinkedHashMap<>();

        try (var in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            LOGGER.trace("Skipping {} magic bytes", Integer.BYTES);
            in.skipBytes(Integer.BYTES);

            int wordCount = in.readInt();

            for (int w = 0; w < wordCount; w++) {
                int len = in.readInt();
                byte[] wordBytes = in.readNBytes(len);
                String word = new String(wordBytes, StandardCharsets.UTF_8);

                List<LocalDateTime> timestamps = new ArrayList<>();

                int dayCount = in.readInt();

                for (int d = 0; d < dayCount; d++) {
                    int dayInt = in.readInt();
                    String day = formatDay(dayInt);

                    int hourCount = in.readByte();

                    for (int h = 0; h < hourCount; h++) {
                        int hour = in.readByte();

                        int count = in.readInt();

                        int base = in.readInt();
                        int prev = base;

                        timestamps.add(toDateTime(day, hour, base));

                        for (int i = 1; i < count; i++) {
                            int delta = in.readInt();
                            int value = prev + delta;

                            timestamps.add(toDateTime(day, hour, value));
                            prev = value;
                        }
                    }
                }
                map.put(word, timestamps);
                LOGGER.trace("Loaded word '{}' with {} entries.", word, timestamps.size());
            }
            LOGGER.debug("Binary load complete. Categories: {}", map.size());
        } catch (EOFException e) {
            LOGGER.warn("Reached unexpected end of binary file: {}. Data might be incomplete.", file.getName(), e);
        } catch (Exception e) {
            LOGGER.error("Critical failure reading binary file: {}", file.getAbsolutePath(), e);
        }

        return map;
    }

    @Override
    public boolean isType(File file) {
        try (var in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            int readValue = in.readInt();
            boolean match = readValue == MAGIC_BYTES;
            LOGGER.trace("Reading first {}B = {}; Is decided format", Integer.BYTES, readValue, match);
            return match;
        } catch (Exception e) {
            LOGGER.error("File {} is not a valid {} binary file", MAGIC_BYTES, file.getName());
        }

        return false;
    }

    private static String formatDay(int day) {
        try {
            String s = String.valueOf(day);
            return s.substring(0, 4) + "-" + s.substring(4, 6) + "-" + s.substring(6);
        } catch (Exception e) {
            LOGGER.warn("Failed to format day integer: {}. Stream might be misaligned.", day);
            return "0000-00-00";
        }
    }

    private static LocalDateTime toDateTime(String day, int hour, int value) {
        try {
            String padded = String.format("%07d", value);

            String time =
                    padded.substring(0, 2) + ":" +
                            padded.substring(2, 4) + ":" +
                            padded.substring(4);

            return LocalDateTime.parse(
                    "%s %02d:%s".formatted(day, hour, time),
                    java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS")
            );
        } catch (Exception e) {
            LOGGER.error("Timestamp parsing error for day {} hour {} value {}.", day, hour, value, e);
            throw e;
        }
    }
}
