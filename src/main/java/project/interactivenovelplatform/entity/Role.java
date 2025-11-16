package project.interactivenovelplatform.entity;

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

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
