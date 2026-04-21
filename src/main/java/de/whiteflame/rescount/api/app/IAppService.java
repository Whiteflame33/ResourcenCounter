package de.whiteflame.rescount.api.app;

public interface IAppService {
    void increment(String key);
    int getCount(String key);
    void addListener(IAppListener listener);
    void shutdown();

    @FunctionalInterface
    interface IAppListener {
        void onUpdate(String key, int value);
    }
}
