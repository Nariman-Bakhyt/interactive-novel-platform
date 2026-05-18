package project.interactivenovelplatform.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import project.interactivenovelplatform.config.RateLimited;
import project.interactivenovelplatform.dto.request.ChangePasswordRequestDto;
import project.interactivenovelplatform.dto.request.UserSettingsRequestDto;
import project.interactivenovelplatform.dto.request.UserUpdateRequestDto;
import project.interactivenovelplatform.dto.response.ProfileResponseDto;
import project.interactivenovelplatform.dto.response.UserResponseDto;
import project.interactivenovelplatform.dto.response.UserSettingsResponseDto;
import project.interactivenovelplatform.security.UserPrincipal;
import project.interactivenovelplatform.service.StorageService;
import project.interactivenovelplatform.service.UserService;

import java.security.Principal;

@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController {
    private final UserService userService;
    private final StorageService storageService;

    @RateLimited(capacity = 20, minutes = 1)
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileResponseDto> getMyProfile(@AuthenticationPrincipal UserPrincipal principal) {
        var user = userService.findProfileById(principal.getId(),principal.getId());
        return ResponseEntity.ok().body(user);
    }
    @RateLimited(capacity = 30, minutes = 1)
    @GetMapping("/profile/{userId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileResponseDto> getProfile(@PathVariable Long userId,@AuthenticationPrincipal UserPrincipal principal) {
        var user = userService.findProfileById(principal.getId(),userId);
        return ResponseEntity.ok().body(user);
    }
    @RateLimited(capacity = 5, minutes = 10)
    @PostMapping("/me/avatar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileResponseDto> uploadUserAvatar(@RequestParam(value = "file", required = false) MultipartFile file, @AuthenticationPrincipal UserPrincipal principal) {
        var user = userService.uploadUserAvatar(file, principal);
        return ResponseEntity.ok().body(user);
    }
    @RateLimited(capacity = 3, minutes = 15)
    @PostMapping("/me/update")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ProfileResponseDto> updateProfile(Authentication authentication ,@Valid @RequestBody UserUpdateRequestDto dto){
        Long id = ((UserPrincipal) authentication.getPrincipal()).getId();
        var user = userService.updateProfileDetails(id, dto);
        return ResponseEntity.ok().body(user);
    }
    @RateLimited(capacity = 3, minutes = 15)
    @PostMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword ( Authentication authentication, @Valid @RequestBody ChangePasswordRequestDto changePasswordRequestDto ){
        Long id = ((UserPrincipal) authentication.getPrincipal()).getId();
        userService.changePassword(id, changePasswordRequestDto);
        return ResponseEntity.ok().build();
    }
    @RateLimited(capacity = 40, minutes = 1)
    @GetMapping("/public/search")
    public ResponseEntity<PagedModel<UserResponseDto>> findByUsername(
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size){
        if (size > 50) size = 50;
        Page<UserResponseDto> body = userService.searchUsers(page,size,username);
        return ResponseEntity.ok().body(new PagedModel<>(body));
    }
    @RateLimited(capacity = 20, minutes = 1)
    @GetMapping("/setting")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserSettingsResponseDto> getUserSettings(Authentication authentication) {
        var user = userService.getUserSettings(((UserPrincipal) authentication.getPrincipal()).getId());
        return ResponseEntity.ok().body(user);
    }
    @RateLimited(capacity = 10, minutes = 60)
    @PatchMapping("/setting")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserSettingsResponseDto> updateUserSettings(Authentication authentication, @RequestBody UserSettingsRequestDto dto) {
        var user = userService.updateUserSettings(((UserPrincipal) authentication.getPrincipal()).getId(), dto);
        return  ResponseEntity.ok().body(user);
    }

//    @GetMapping("/me")
//    public ResponseEntity<UserResponseDto> getMyProfile(Principal principal) {
//        // Principal.getName() берет имя из JWT токена
//        return ResponseEntity.ok(userService.findByUsername(principal.getName()));
//    }


//    @GetMapping("/login")
//    public String logout(Map<String, Object> model, CsrfToken csrfToken){
//        return "login";
//    }


//    @GetMapping("/reg")
//    public String regStart(
//            Map<String, Object> model, CsrfToken csrfToken
//    ){
//        return "reg";
//    }
//    @PostMapping("/reg")
//    public String regEnd(
//            @ModelAttribute @Valid RegistrationRequestDto registrationRequestDto,
//            BindingResult bindingResult,
//            Map<String, Object> model, CsrfToken csrfToken
//    ){
//        if (bindingResult.hasErrors()) {
//            // Добавляем ошибки в модель
//            model.put("errors", bindingResult.getAllErrors());
//            // Возвращаем ту же форму, чтобы пользователь исправил ошибки
//            var uuser= userService.findAll();
//            model.put("uuser", uuser);
//            return "reg";
//        }
//        userService.registerUser(registrationRequestDto);
//        return "reg";
//    }

//    @GetMapping("/main")
//    public String main(
//            Map<String,Object> model, CsrfToken csrfToken
//    ){
//        var uuser= userService.findAll();
//        model.put("uuser",uuser);
//        return "registration";
//    }
//
//    @PostMapping("/main")
//    public String addUser(
//            @ModelAttribute @Valid RegistrationRequestDto registrationRequestDto,
//            BindingResult bindingResult,
//            Map<String, Object> model, CsrfToken csrfToken
//    ){
//        if (bindingResult.hasErrors()) {
//            // Добавляем ошибки в модель
//            model.put("errors", bindingResult.getAllErrors());
//            // Возвращаем ту же форму, чтобы пользователь исправил ошибки
//            var uuser= userService.findAll();
//            model.put("uuser", uuser);
//            return "registration";
//        }
//        userService.registerUser(registrationRequestDto);
//
//        var uuser= userService.findAll();
//        model.put("uuser", uuser);
//        return "registration";
//    }

}