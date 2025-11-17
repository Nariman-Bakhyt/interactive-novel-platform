package project.interactivenovelplatform.service.impl;

import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationFailureBadCredentialsEvent;
import org.springframework.stereotype.Component;
import project.interactivenovelplatform.service.UserService;

@Component
public class AuthenticationFailureListener implements ApplicationListener<AuthenticationFailureBadCredentialsEvent> {

    private final UserService userService; // Внедряем ваш сервис

    // Конструктор
    public AuthenticationFailureListener(UserService userService) {
        this.userService = userService;
    }

    // 💡 Этот метод вызывается каждый раз при неудачной попытке входа
    @Override
    public void onApplicationEvent(AuthenticationFailureBadCredentialsEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        String username;
        if (principal instanceof String principalName) {
            username = principalName;
        } else {
            username = principal.toString();
        }
        userService.handleFailedLogin(username);
    }
}
