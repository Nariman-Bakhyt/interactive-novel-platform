package project.interactivenovelplatform.service.impl;

import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import project.interactivenovelplatform.service.UserService;
@Component
public class AuthenticationSuccessListener implements ApplicationListener<AuthenticationSuccessEvent> {
    private final UserService userService; // Внедряем ваш сервис
    public AuthenticationSuccessListener(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails userDetails) {
            // Spring Security обычно возвращает объект UserDetails
            username = userDetails.getUsername();
        } else if (principal instanceof String principalName) {
            // Если Principal - это просто строка
            username = principalName;
        } else {
            // Если объект Principal другой, пропускаем
            return;
        }
        userService.handleSuccessfulLogin(username);
    }
}
