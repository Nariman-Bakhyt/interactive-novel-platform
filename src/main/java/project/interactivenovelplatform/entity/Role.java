package project.interactivenovelplatform.entity;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
public enum Role {
    // 1. Константа enum по соглашению (UPPER_SNAKE_CASE)
    THE_MAKER("The Maker"),
    SUPER_ADMIN("Super Admin"),
    ADMIN("Administrator"),
    USER("User");

    private final String displayName;

    Role(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
