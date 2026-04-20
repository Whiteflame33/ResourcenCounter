package de.whiteflame.rescount.api.service;

@FunctionalInterface
public interface ICounterListener {
    void onUpdate(String key, int value);
}
