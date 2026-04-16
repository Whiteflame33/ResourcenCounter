package de.whiteflame.rescount;

import de.whiteflame.rescount.api.model.GroupedDay;
import de.whiteflame.rescount.api.model.GroupedHour;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class TimestampGrouper {
    private TimestampGrouper() {}

    public static List<GroupedDay> group(List<LocalDateTime> timestamps) {
        timestamps.sort(null);

        Map<Integer, Map<Integer, List<Integer>>> grouped = new LinkedHashMap<>();

        for (var ts : timestamps) {

            int day = ts.getYear() * 10000
                    + ts.getMonthValue() * 100
                    + ts.getDayOfMonth();

            int hour = ts.getHour();

            int time = ts.getMinute() * 100000
                    + ts.getSecond() * 1000
                    + ts.getNano() / 1_000_000;

            grouped
                    .computeIfAbsent(day, d -> new LinkedHashMap<>())
                    .computeIfAbsent(hour, h -> new ArrayList<>())
                    .add(time);
        }

        List<GroupedDay> result = new ArrayList<>();

        for (var dayEntry : grouped.entrySet()) {
            List<GroupedHour> hours = new ArrayList<>();

            for (var hourEntry : dayEntry.getValue().entrySet()) {
                List<Integer> times = hourEntry.getValue();

                times.sort(Integer::compare);

                hours.add(new GroupedHour(hourEntry.getKey(), times));
            }

            result.add(new GroupedDay(dayEntry.getKey(), hours));
        }

        return result;
    }
}
