package project.interactivenovelplatform.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SubscribableType {
    FORUM_TOPIC(Values.FORUM_TOPIC),
    CHANNEL(Values.CHANNEL);
    SubscribableType(String value) {}
    // Определяем константы прямо здесь
    public static class Values {
        public static final String FORUM_TOPIC = "FORUM_TOPIC";
        public static final String CHANNEL = "CHANNEL";
    }
}
