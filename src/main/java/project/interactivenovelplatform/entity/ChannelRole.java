package project.interactivenovelplatform.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ChannelRole {
    ADMIN("Admin", 1),
    MODERATOR("Moderator", 2),
    SUBSCRIBER("Subscriber", 3);

    private final String displayName;
    private final int rank;

}
