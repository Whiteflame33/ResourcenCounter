package de.whiteflame.rescount.app;

import de.whiteflame.rescount.api.app.IAppService;
import de.whiteflame.rescount.api.service.ICounterListener;
import de.whiteflame.rescount.api.service.ICounterService;
import de.whiteflame.rescount.config.GlobalConfig;
import de.whiteflame.rescount.io.FileConstants;
import de.whiteflame.rescount.io.FileHandler;

import java.util.ArrayList;
import java.util.List;

public class LocalAppService implements IAppService, ICounterListener {
    private final ICounterService service;
    private final FileHandler fileHandler;
    private final GlobalConfig config;
    private final List<IAppListener> listeners = new ArrayList<>();

    public LocalAppService(ICounterService service, FileHandler handler, GlobalConfig config) {
        this.service = service;
        this.fileHandler = handler;
        this.config = config;
        service.addListener(this);
    }

    @Override
    public void increment(String key) {
        service.increment(key);
    }

    @Override
    public int getCount(String key) {
        return service.getCount(key);
    }

    @Override
    public void addListener(IAppListener listener) {
        listeners.add(listener);
    }

    @Override
    public void shutdown() {
        if (!service.hasChangedSince())
            return;

        try {
            fileHandler.save(
                    service.getEntries(),
                    config.getDataFile(FileConstants.COMPRESSED_DATA_FILE_EXT),
                    config.getAs(GlobalConfig.DATA_TYPE)
            );;
        } catch (Exception e) {

        }
    }

    @Override
    public void onUpdate(String key, int value) {
        for (var l : listeners) {
            l.onUpdate(key, value);
        }
    }
}
