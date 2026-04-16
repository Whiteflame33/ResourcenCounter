package de.whiteflame.rescount.io.impl.xml.model;

import java.util.Set;

public record XmlModelDay(String value, Set<XmlModelHour> hours) {}
