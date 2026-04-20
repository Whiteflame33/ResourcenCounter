package de.whiteflame.rescount.service.impl;

import de.whiteflame.rescount.api.log.ILogger;
import de.whiteflame.rescount.api.log.LoggerFactory;
import de.whiteflame.rescount.api.service.ICounterListener;
import de.whiteflame.rescount.api.service.ICounterService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class CounterService implements ICounterService {
    private static final ILogger LOGGER = LoggerFactory.getLogger(CounterService.class);

    private final List<ICounterListener> listeners = new ArrayList<>();
    private final Map<String, List<LocalDateTime>> entries = new LinkedHashMap<>();
    private boolean entries_changed = false;

    @Override
    public synchronized int getCount(String key) {
        int size = entries.getOrDefault(key, List.of()).size();
        LOGGER.trace("Getting count of {}; value is {}", key, size);
        return size;
    }

    @Override
    public synchronized void increment(String key) {
        entries
                .computeIfAbsent(key, _ -> new ArrayList<>())
                .add(LocalDateTime.now());
        entries_changed = true;
        int newSize = entries.get(key).size();
        notifyListeners(key, newSize);
        LOGGER.trace("Incrementing count of {}; value is {}", key, newSize);
    }

    @Override
    public synchronized Map<String, List<LocalDateTime>> getEntries() {
        return Map.copyOf(entries);
    }

    @Override
    public synchronized void setEntries(Map<String, List<LocalDateTime>> loaded) {
        LOGGER.trace("Setting entries");
        entries.clear();
        entries.putAll(loaded);
        entries_changed = false;
    }

    @Override
    public boolean hasChangedSince() {
        return entries_changed;
    }

    @Override
    public void addListener(ICounterListener l) {
        listeners.add(l);
    }

    @Override
    public void notifyListeners(String key, int value) {
        for (var l : listeners)
            l.onUpdate(key, value);
    }
}
