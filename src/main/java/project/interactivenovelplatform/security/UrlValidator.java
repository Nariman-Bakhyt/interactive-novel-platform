package project.interactivenovelplatform.security;

import java.net.URI;
import java.util.List;

public class UrlValidator {
    private static final List<String> ALLOWED_DOMAINS = List.of(
            "localhost:5173",
            "interactivenovel.ru",
            "localhost:9000",
            "127.0.0.1:9000"
    );

    public static boolean isTrusted(String url) {
        if (url == null || url.isBlank()) return false;

        try {
            // Если протокола нет, добавляем http:// (так как у тебя пока http)
            String testUrl = url.contains("://") ? url : "http://" + url;

            URI uri = new URI(testUrl);
            String host = uri.getHost();
            int port = uri.getPort();

            // Если порт стандартный для http (80), URI.getPort() вернет -1
            // Нам нужно это учитывать при сравнении с localhost:5173
            String hostWithPort = (port != -1) ? host + ":" + port : host;

            return ALLOWED_DOMAINS.stream().anyMatch(domain -> {
                return domain.equalsIgnoreCase(hostWithPort) || domain.equalsIgnoreCase(host);
            });
        } catch (Exception e) {

            return false;
        }
    }
}
