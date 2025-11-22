package project.interactivenovelplatform.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum Role {
    // 1. Константа enum по соглашению (UPPER_SNAKE_CASE)
    THE_MAKER("The Maker"),
    SUPER_ADMIN("Super Admin"),
    ADMIN("Administrator"),
    USER("User");

    private final String displayName;
    @Override
    public String toString() {
        return displayName;
    }
}
