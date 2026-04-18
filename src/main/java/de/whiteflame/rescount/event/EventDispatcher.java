package de.whiteflame.rescount.event;

import de.whiteflame.rescount.api.event.EventType;
import de.whiteflame.rescount.api.event.IEvent;
import de.whiteflame.rescount.api.log.ILogger;
import de.whiteflame.rescount.api.log.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class EventDispatcher {
    private static final ILogger LOGGER = LoggerFactory.getLogger(EventDispatcher.class);
    private final Map<EventType, Consumer<IEvent>> handlers = new HashMap<>();

    public void registerHandler(EventType type, Consumer<IEvent> handler) {
        LOGGER.info("Registering event handler {} to {}", handler.getClass().getName(), type.name());
        handlers.put(type, handler);
    }

    public void dispatch(IEvent event) {
        Consumer<IEvent> handler = handlers.get(event.getType());
        if (handler != null) {
            LOGGER.info("Dispatching event to {}", handler.getClass().getName());
            handler.accept(event);
        }
    }
}
