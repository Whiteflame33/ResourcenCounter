package de.whiteflame.rescount.io.impl;

import de.whiteflame.rescount.TimestampGrouper;
import de.whiteflame.rescount.api.io.FileType;
import de.whiteflame.rescount.api.io.IFileReader;
import de.whiteflame.rescount.api.io.IFileWriter;
import de.whiteflame.rescount.api.model.GroupedHour;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CompressedBinaryFileImpl implements IFileReader, IFileWriter {
    private static final byte[] MAGIC_BYTES = { 'C', 'W', 'T', 'B', 'S' };

    @Override
    public FileType getFileType() {
        return FileType.BYTE_2;
    }

    @Override
    public void writeFile(File file, Map<String, List<LocalDateTime>> groupedTimestamps) {
        int totalEntries = 0;
        try (var out = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(file)))) {
            out.write(MAGIC_BYTES);
            writeVarInt(out, groupedTimestamps.size());

            for (var entry : groupedTimestamps.entrySet()) {
                byte[] wordBytes = entry.getKey().getBytes(StandardCharsets.UTF_8);
                writeVarInt(out, wordBytes.length);
                out.write(wordBytes);

                var grouped = TimestampGrouper.group(entry.getValue());

                writeVarInt(out, grouped.size());

                int prevYear = -1;

                for (var year : grouped) {

                    int currentYear = year.year();
                    writeVarInt(out, currentYear - prevYear);
                    prevYear = currentYear;

                    writeVarInt(out, year.days().size());

                    int prevDay = -1;

                    for (var day : year.days()) {
                        int currentDay = day.day();
                        writeVarInt(out, currentDay - prevDay);
                        prevDay = currentDay;

                        List<Integer> times = compressTimes(day.hours());
                        writeVarInt(out, times.size());
                        totalEntries += times.size();

                        if (times.isEmpty())
                            continue;

                        var it = times.iterator();

                        int base = it.next();
                        writeVarInt(out, base);

                        if (!it.hasNext())
                            continue;

                        int prev = base;
                        int prevDelta = 0;

                        int current = it.next();
                        int delta = current - prev;

                        writeVarInt(out, zigZagEncode(delta));
                        prev = current;
                        prevDelta = delta;

                        while (it.hasNext()) {

                            current = it.next();

                            int newDelta = current - prev;
                            int deltaOfDelta = newDelta - prevDelta;

                            writeVarInt(out, zigZagEncode(deltaOfDelta));

                            prev = current;
                            prevDelta = newDelta;
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

        try (var in = new DataInputStream(new BufferedInputStream(new FileInputStream(file)))) {
            byte[] header = in.readNBytes(MAGIC_BYTES.length);
            for (int i = 0; i < MAGIC_BYTES.length; i++) {
                if (header[i] != MAGIC_BYTES[i]) {
                    return map;
                }
            }

            int wordCount = readVarInt(in);

            for (int w = 0; w < wordCount; w++) {
                int len = readVarInt(in);
                byte[] wordBytes = in.readNBytes(len);
                String word = new String(wordBytes, StandardCharsets.UTF_8);

                List<LocalDateTime> timestamps = new ArrayList<>();
                int yearCount = readVarInt(in);
                int prevYear = -1;

                for (int y = 0; y < yearCount; y++) {
                    int year = (prevYear == -1) ? readVarInt(in) : prevYear + readVarInt(in);
                    prevYear = year;

                    int dayCount = readVarInt(in);
                    int prevDay = -1;

                    for (int d = 0; d < dayCount; d++) {
                        int day = (prevDay == -1) ? readVarInt(in) : prevDay + readVarInt(in);
                        prevDay = day;

                        // FIX: The writer flattens all hours, so we read ONE count per day,
                        // NOT an hourCount loop.
                        int count = readVarInt(in);
                        if (count <= 0) continue;

                        LocalDate date = LocalDate.ofYearDay(year, day);

                        // ---- BASE ----
                        int value = readVarInt(in);
                        timestamps.add(LocalDateTime.of(date, extractTime(value)));

                        if (count > 1) {
                            // ---- FIRST DELTA ----
                            int prevDelta = zigZagDecode(readVarInt(in));
                            value += prevDelta;
                            timestamps.add(LocalDateTime.of(date, extractTime(value)));

                            // ---- DELTA-OF-DELTA ----
                            for (int i = 2; i < count; i++) {
                                int deltaOfDelta = zigZagDecode(readVarInt(in));
                                int delta = prevDelta + deltaOfDelta;
                                value += delta;
                                timestamps.add(LocalDateTime.of(date, extractTime(value)));
                                prevDelta = delta;
                            }
                        }
                    }
                }
                map.put(word, timestamps);
            }
        } catch (EOFException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return map;
    }

    @Override
    public boolean isType(File file) {
        try (var in = new DataInputStream(
                new BufferedInputStream(new FileInputStream(file)))) {

            byte[] magicWords = new byte[MAGIC_BYTES.length];
            int magic = in.readNBytes(magicWords, 0, MAGIC_BYTES.length);

            if (magic != MAGIC_BYTES.length)
                return false;

            for (int i = 0; i < MAGIC_BYTES.length; i++) {
                if (magicWords[i] != MAGIC_BYTES[i])
                    return false;
            }
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static List<Integer> compressTimes(List<GroupedHour> hours) {
        List<Integer>  times = new ArrayList<>();
        for (var hour : hours) {
            int h = hour.hour();

            for (int t : hour.times()) {
                int minute = t / 100_000;
                int second = (t / 1000) % 100;
                int millis = t % 1000;

                int msOfDay =
                        h * 3_600_000 +
                                minute * 60_000 +
                                second * 1_000 +
                                millis;

                times.add(msOfDay);
            }
        }
        times.sort(Integer::compare);

        return times;
    }

    private static LocalTime extractTime(int msOfDay) {
        int hour = msOfDay / 3_600_000;
        int rem = msOfDay % 3_600_000;
        int minute = rem / 60_000;
        rem %= 60_000;
        int second = rem / 1000;
        int millis = rem % 1000;

        return LocalTime.of(Math.min(23, hour), minute, second, millis * 1_000_000);
    }

    private static void writeVarInt(DataOutput out, int value) throws Exception {
        while ((value & 0xFFFF_FF80) != 0L) {
            out.writeByte((value & 0x7F) | 0x80);
            value >>>= 7;
        }
        out.writeByte(value & 0x7F);
    }

    private static int readVarInt(DataInputStream in) throws Exception {
        int value = 0;
        int position = 0;
        byte b;

        while (true) {
            b = in.readByte();
            value |= (b & 0x7F) << position;

            if ((b & 0x80) == 0)
                break;

            position += 7;

            if (position > 35) {
                throw new RuntimeException("VarInt too long (corrupted stream)");
            }
        }

        return value;
    }

    private static int zigZagEncode(int n) {
        return (n << 1) ^ (n >> 31);
    }

    private static int zigZagDecode(int n) {
        return (n >>> 1) ^ -(n & 1);
    }

    private static LocalTime toDateTime(int hour, int value) {
        try {
            String padded = String.format("%07d", value);

            int minute = Integer.parseInt(padded.substring(0, 2));
            int second = Integer.parseInt(padded.substring(2, 4));
            int msecnd = Integer.parseInt(padded.substring(4));

            return LocalTime.of(hour, minute, second, msecnd);
        } catch (Exception e) {
            return LocalTime.of(hour, 0, 0, 0);
        }
    }
}
