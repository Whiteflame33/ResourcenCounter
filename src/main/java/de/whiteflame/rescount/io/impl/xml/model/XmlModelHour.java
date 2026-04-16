package de.whiteflame.rescount.io.impl.xml.model;

import java.util.Set;

public record XmlModelHour(String value, Set<XmlModelTime> times) {}
