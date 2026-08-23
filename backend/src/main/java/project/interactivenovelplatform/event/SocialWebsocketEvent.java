package project.interactivenovelplatform.event;

import org.springframework.context.ApplicationEvent;
import project.interactivenovelplatform.dto.response.WsEventDto;

public class SocialWebsocketEvent extends ApplicationEvent {
    private final String destination;
    private final WsEventDto<?> payload;

    public SocialWebsocketEvent(Object source, String destination, WsEventDto<?> payload) {
        super(source);
        this.destination = destination;
        this.payload = payload;
    }

    public String getDestination() {
        return destination;
    }

    public WsEventDto<?> getPayload() {
        return payload;
    }
}
