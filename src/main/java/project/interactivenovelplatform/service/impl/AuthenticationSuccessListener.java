package project.interactivenovelplatform.service.impl;

import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import project.interactivenovelplatform.entity.AppUserEntity;
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
        if (principal instanceof AppUserEntity appUser) {
            // Теперь у вас есть доступ к объекту AppUserEntity
            String username = appUser.getUsername();
            // ... Здесь ваша логика, например, сброс счетчика failed_attempt_count
            System.out.println("Успешный вход для пользователя: " + username);

        } else if (principal instanceof String principalName) {
            // Если principal все же является String (например, имя пользователя)
            System.out.println("Успешный вход для имени: " + principalName);
        }
    }
}
