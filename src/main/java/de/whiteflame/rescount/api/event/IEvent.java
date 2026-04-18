package de.whiteflame.rescount.api.event;

import java.time.LocalDateTime;
import java.util.UUID;

public interface IEvent {
    UUID getEventId();
    String getSourceId();
    LocalDateTime getTimestamp();
    EventType getType();
}
