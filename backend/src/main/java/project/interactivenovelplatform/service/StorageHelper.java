package project.interactivenovelplatform.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class StorageHelper {

    @Value("${app.storage.default-avatar}")
    private String defaultAvatar;

    @Value("${app.storage.default-cover}")
    private String defaultCover;

    public String getAvatarOrDefault(String url) {
        if (url == null || url.trim().isEmpty()) {
            return defaultAvatar;
        }
        return url;
    }

    public String getCoverOrDefault(String url) {
        if (url == null || url.trim().isEmpty()) {
            return defaultCover;
        }
        return url;
    }
}