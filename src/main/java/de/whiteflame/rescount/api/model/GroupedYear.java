package de.whiteflame.rescount.api.model;

import java.util.List;

public record GroupedYear(int year, List<GroupedDay> days) {}
