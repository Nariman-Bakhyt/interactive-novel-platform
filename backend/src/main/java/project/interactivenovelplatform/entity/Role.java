package project.interactivenovelplatform.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;


@Getter
@AllArgsConstructor
public enum Role {
    THE_MAKER("The Maker", 0),
    SUPER_ADMIN("Super Admin", 1),
    ADMIN("Administrator", 2),
    USER("User", 3);

    private final String displayName;
    private final int rank;

    @Override
    public String toString() {
        return displayName;
    }
}
