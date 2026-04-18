package de.whiteflame.rescount.event;

import de.whiteflame.rescount.api.log.ILogger;
import de.whiteflame.rescount.api.log.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class EventProcessor {
    private static final ILogger LOGGER = LoggerFactory.getLogger(EventProcessor.class);

    private final Map<String, List<LocalDateTime>> state;
    private final Set<UUID> processedEvents = new HashSet<>();

    public EventProcessor(Map<String, List<LocalDateTime>> initialState) {
        this.state = initialState;
    }

    public synchronized boolean process(WordCountEvent event) {
        LOGGER.debug("Processing event {}", event);

        if (processedEvents.contains(event.eventId())) {
            LOGGER.trace("Skipping event {}; already processed", event);
            return false;
        }

        LOGGER.trace("Mark event as processed");
        state.computeIfAbsent(event.word(), _ -> new ArrayList<>())
                .add(event.timestamp());

        processedEvents.add(event.getEventId());
        return true;
    }
}
