package project.interactivenovelplatform.controller;

import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import project.interactivenovelplatform.config.RateLimited;
import project.interactivenovelplatform.dto.request.*;
import project.interactivenovelplatform.dto.response.AuthResponseDto;
import project.interactivenovelplatform.dto.response.JwtAuthenticationResponseDto;
import project.interactivenovelplatform.entity.AppUserEntity;
import project.interactivenovelplatform.entity.VerificationTokenType;
import project.interactivenovelplatform.security.UserPrincipal;
import project.interactivenovelplatform.service.AuthService;
import project.interactivenovelplatform.service.UserService;
import project.interactivenovelplatform.service.VerificationService;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final VerificationService verificationService;
    private final AuthService authService;


    @RateLimited(capacity = 5, minutes = 5)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody @Valid LoginRequestDto loginRequest, HttpServletRequest request){
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);
        UserPrincipal user = (UserPrincipal) authentication.getPrincipal();

        AuthResponseDto authResponse = authService.createSession(user, request);

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authResponse.getCookie().toString())
                .header(HttpHeaders.SET_COOKIE, authResponse.getGuestCookie().toString())
                .body(new JwtAuthenticationResponseDto(authResponse.getAccessToken(), authResponse.getUsername()));
    }

    @RateLimited(capacity = 3, minutes = 10)
    @PostMapping("/login/email")
    public ResponseEntity<String> requestLoginCode(@RequestBody @Valid EmailRequestDto loginRequest) {
        try {
            AppUserEntity user = userService.getEntityByEmail(loginRequest.getEmail());
            verificationService.sendVerificationCode(user.getId(), VerificationTokenType.LOGIN_BY_CODE, null);
        } catch (EntityNotFoundException e) {
        }
        return ResponseEntity.ok("Код подтверждения отправлен на почту");
    }

    
    @RateLimited(capacity = 5, minutes = 5)
    @PostMapping("/login/verify")
    public ResponseEntity<?> verifyLoginCode(@RequestBody @Valid VerifyLoginCodeRequestDto verifyRequest, HttpServletRequest request) {
        AuthResponseDto authResponse = authService.loginByVerificationCode(
                verifyRequest.getEmail(),
                verifyRequest.getCode(),
                request
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authResponse.getCookie().toString())
                .header(HttpHeaders.SET_COOKIE, authResponse.getGuestCookie().toString())
                .body(new JwtAuthenticationResponseDto(
                        authResponse.getAccessToken(),
                        authResponse.getUsername()
                ));
    }


    @RateLimited(capacity = 5, minutes = 15)
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@CookieValue(name = "refreshToken", required = false) String refreshToken,
                                          HttpServletRequest request) {
        try {
            String userAgent = request.getHeader("User-Agent");
            JwtAuthenticationResponseDto response = authService.refreshAccessToken(refreshToken, userAgent);
            return ResponseEntity.ok(response);
        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error refreshing token");
        }
    }

    @RateLimited(capacity = 15, minutes = 5)
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> logoutUser(@CookieValue(name = "refreshToken", required = false) String refreshToken) {
        authService.logout(refreshToken);
        ResponseCookie cookie = ResponseCookie.from("refreshToken", "")
                .maxAge(0)
                .secure(false)
                .httpOnly(true)
                .path("/api/auth")
                .sameSite("Lax")
                .build();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    @RateLimited(capacity = 3, minutes = 10)
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@RequestBody @Valid RegistrationRequestDto registrationRequestDto){
        var user = userService.registerUser(registrationRequestDto);
        verificationService.sendVerificationCode(user.getId(), VerificationTokenType.REGISTRATION_CONFIRMATION, null);
        return ResponseEntity.status(HttpStatus.CREATED).body(user.getId());
    }
    @RateLimited(capacity = 3, minutes = 10)
    @PostMapping("/register/verify-code")
    public ResponseEntity<?> verifyRegister(
            HttpServletRequest request,
            @RequestBody @Valid VerificationRequestDto dto) {

        userService.verifyCode(dto);

        return ResponseEntity.ok("Действие успешно подтверждено.");
    }

    @PostMapping("/password/reset-request")
    public ResponseEntity<String> requestPasswordReset(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid ResetPasswordRequestDto dto) {

        userService.forgotPassword(principal.getId(),dto);
        return ResponseEntity.ok("Код подтверждения для смены пароля отправлен на почту.");
    }

    
    @PostMapping("/verify-code")
    public ResponseEntity<String> verifyCode(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid VerificationRequestDto dto) {
        dto.setUserId(principal.getId());
        userService.verifyCode(dto);

        return ResponseEntity.ok("Действие успешно подтверждено.");
    }

    
    @PostMapping("/email/update-request")
    public ResponseEntity<String> requestEmailUpdate(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody @Valid EmailRequestDto dto) {

        userService.updateEmail(principal.getId(), dto);

        return ResponseEntity.ok("Код подтверждения отправлен на новый адрес электронной почты.");
    }

}

