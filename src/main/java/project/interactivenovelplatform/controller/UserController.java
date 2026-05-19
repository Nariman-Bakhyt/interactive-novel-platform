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
import project.interactivenovelplatform.service.UserService;

@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController {
    private final UserService userService;

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




































































}