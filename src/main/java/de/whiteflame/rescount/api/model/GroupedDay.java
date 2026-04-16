package de.whiteflame.rescount.api.model;

import java.util.List;

public record GroupedDay(int day, List<GroupedHour> hours) {}
