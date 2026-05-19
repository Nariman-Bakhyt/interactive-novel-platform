package project.interactivenovelplatform.service.impl;

import org.springframework.context.ApplicationListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import project.interactivenovelplatform.service.UserService;
@Component
public class AuthenticationSuccessListener implements ApplicationListener<AuthenticationSuccessEvent> {
    private final UserService userService; 
    public AuthenticationSuccessListener(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void onApplicationEvent(AuthenticationSuccessEvent event) {
        Object principal = event.getAuthentication().getPrincipal();
        String username;
        if (principal instanceof UserDetails userDetails) {
            
            username = userDetails.getUsername();
        } else if (principal instanceof String principalName) {
            
            username = principalName;
        } else {
            
            return;
        }
        userService.handleSuccessfulLogin(username);
    }
}
