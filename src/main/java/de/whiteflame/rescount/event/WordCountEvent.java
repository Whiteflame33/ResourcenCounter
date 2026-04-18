package de.whiteflame.rescount.event;

import de.whiteflame.rescount.api.event.EventType;
import de.whiteflame.rescount.api.event.IEvent;
import de.whiteflame.rescount.api.log.ILogger;
import de.whiteflame.rescount.api.log.LoggerFactory;

import java.time.LocalDateTime;
import java.util.UUID;

public record WordCountEvent(
        UUID eventId,
        String sourceId,
        String word,
        LocalDateTime timestamp
) implements IEvent {
    private static final ILogger LOGGER = LoggerFactory.getLogger(WordCountEvent.class);

    public static WordCountEvent create(String sourceId, String word) {
        LOGGER.trace("Creating word count event with sourceId {} and word {}", sourceId, word);
        return new WordCountEvent(UUID.randomUUID(), sourceId, word, LocalDateTime.now());
    }


    @Override
    public UUID getEventId() {
        return null;
    }

    @Override
    public String getSourceId() {
        return "";
    }

    @Override
    public LocalDateTime getTimestamp() {
        return null;
    }

    @Override
    public EventType getType() {
        return null;
    }
}
