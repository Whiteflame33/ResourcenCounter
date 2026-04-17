package de.whiteflame.rescount.io.impl;

import de.whiteflame.rescount.TimestampGrouper;
import de.whiteflame.rescount.api.io.FileType;
import de.whiteflame.rescount.api.io.IFileReader;
import de.whiteflame.rescount.api.io.IFileWriter;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
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
    private static final int MAGIC_BYTES = ('W' << 24) | ('T' << 16) | ('B' << 8) | 'S';

    @Override
    public FileType getFileType() {
        return FileType.BYTE_1;
    }

    @Override
    public void writeFile(File file, Map<String, List<LocalDateTime>> groupedTimestamps) {
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

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public Map<String, List<LocalDateTime>> readFile(File file) {
        Map<String, List<LocalDateTime>> map = new LinkedHashMap<>();

        try (var in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
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
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return map;
    }

    @Override
    public boolean isType(File file) {
        try (var in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {
            return in.readInt() == MAGIC_BYTES;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private static String formatDay(int day) {
        String s = String.valueOf(day);
        return s.substring(0,4) + "-" + s.substring(4,6) + "-" + s.substring(6);
    }

    private static LocalDateTime toDateTime(String day, int hour, int value) {
        String padded = String.format("%07d", value);

        String time =
                padded.substring(0, 2) + ":" +
                        padded.substring(2, 4) + ":" +
                        padded.substring(4);

        return LocalDateTime.parse(
                "%s %02d:%s".formatted(day, hour, time),
                java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss:SSS")
        );
    }
}
