package de.whiteflame.rescount.io.impl.xml.model;

import java.util.Set;

public record XmlModelWord(String value, int count, Set<XmlModelDay> days) {}
