package de.whiteflame.rescount.util;

import de.whiteflame.rescount.api.model.GroupedDay;
import de.whiteflame.rescount.api.model.GroupedHour;
import de.whiteflame.rescount.api.model.GroupedYear;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public final class TimestampGrouper {
    private TimestampGrouper() {}

    public static List<GroupedYear> group(List<LocalDateTime> timestamps) {
        timestamps.sort(null);

        List<GroupedYear> years = new ArrayList<>();

        GroupedYear currentYear = null;
        GroupedDay currentDay = null;
        GroupedHour currentHour = null;

        List<Integer> hourTimes = null;
        List<GroupedHour> hours = null;
        List<GroupedDay> days = null;

        int lastYear = -1;
        int lastDay = -1;
        int lastHour = -1;

        for (LocalDateTime ts : timestamps) {

            int year = ts.getYear();
            int day = ts.getDayOfYear();
            int hour = ts.getHour();

            int time = ts.getMinute() * 60000
                    + ts.getSecond() * 1000
                    + ts.getNano() / 1_000_000;

            if (year != lastYear) {
                days = new ArrayList<>();
                currentYear = new GroupedYear(year, days);
                years.add(currentYear);

                lastYear = year;
                lastDay = -1;
                lastHour = -1;
            }

            if (day != lastDay) {
                hours = new ArrayList<>();
                currentDay = new GroupedDay(day, hours);
                days.add(currentDay);

                lastDay = day;
                lastHour = -1;
            }

            if (hour != lastHour) {
                hourTimes = new ArrayList<>();
                currentHour = new GroupedHour(hour, hourTimes);
                hours.add(currentHour);

                lastHour = hour;
            }

            hourTimes.add(time);
        }

        return years;
    }

    public static List<GroupedDay> groupByDay(List<LocalDateTime> timestamps) {
        timestamps.sort(null);

        List<GroupedDay> days = new ArrayList<>();

        GroupedDay currentDay = null;
        GroupedHour currentHour = null;
        List<GroupedHour> hours = null;
        List<Integer> times = null;

        int lastDay = -1;
        int lastHour = -1;

        for (LocalDateTime ts : timestamps) {

            int day = ts.getYear() * 10000
                    + ts.getMonthValue() * 100
                    + ts.getDayOfMonth();

            int hour = ts.getHour();

            int time = ts.getMinute() * 60000
                    + ts.getSecond() * 1000
                    + ts.getNano() / 1_000_000;

            if (day != lastDay) {
                hours = new ArrayList<>();
                currentDay = new GroupedDay(day, hours);
                days.add(currentDay);

                lastDay = day;
                lastHour = -1;
            }

            if (hour != lastHour) {
                times = new ArrayList<>();
                currentHour = new GroupedHour(hour, times);
                hours.add(currentHour);

                lastHour = hour;
            }

            times.add(time);
        }

        return days;
    }
}
