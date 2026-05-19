package project.interactivenovelplatform.security;

import java.net.URI;
import java.util.List;

public class UrlValidator {
    private static final List<String> ALLOWED_DOMAINS = List.of(
            "localhost:5173",
            "http://192.168.8.*:5173",
            "interactivenovel.ru",
            "localhost:9000",
            "http://192.168.8.*:9000",
            "127.0.0.1:9000",
            "localhost",
            "127.0.0.1"
    );

    public static boolean isTrusted(String url) {
        if (url == null || url.isBlank()) return false;

        // 1. Если это относительный путь (начинается с /), значит он наш и доверенный
        if (url.startsWith("/")) {
            return true;
        }

        try {
            // 2. Для абсолютных ссылок исправляем склейку
            String cleanUrl = url.contains("://") ? url : "http://" + url;

            // Убираем возможный двойной слеш после протокола, если пришло "/novels..."
            cleanUrl = cleanUrl.replace("http:///", "http://localhost/");

            URI uri = new URI(cleanUrl);
            String host = uri.getHost();
            if (host == null) return false;

            int port = uri.getPort();
            String hostWithPort = (port != -1) ? host + ":" + port : host;

            return ALLOWED_DOMAINS.stream().anyMatch(domain ->
                    domain.equalsIgnoreCase(hostWithPort) || domain.equalsIgnoreCase(host)
            );
        } catch (Exception e) {
            return false;
        }
    }
}
