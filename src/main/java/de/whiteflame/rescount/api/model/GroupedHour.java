package de.whiteflame.rescount.api.model;

import java.util.List;

public record GroupedHour(int hour, List<Integer> times) {}
