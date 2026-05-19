package project.interactivenovelplatform.event;

import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SocialWebSocketDispatcher {

    private final SimpMessagingTemplate messagingTemplate;

    @EventListener
    public void handleSocialWebsocketEvent(SocialWebsocketEvent event) {
        messagingTemplate.convertAndSend(event.getDestination(), event.getPayload());
    }
}
