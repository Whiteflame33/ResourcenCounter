package de.whiteflame.rescount.api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface ICounterService {
    int getCount(String key);
    void increment(String key);
    Map<String, List<LocalDateTime>> getEntries();
    void setEntries(Map<String, List<LocalDateTime>> entries);
    boolean hasChangedSince();
    void addListener(ICounterListener listener);
    void notifyListeners(String key, int value);
}
