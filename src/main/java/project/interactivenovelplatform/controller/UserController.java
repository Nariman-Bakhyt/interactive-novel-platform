package project.interactivenovelplatform.controller;

import jakarta.validation.Valid;
import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import project.interactivenovelplatform.dto.request.ChangePasswordRequestDto;
import project.interactivenovelplatform.dto.request.UserUpdateRequestDto;
import project.interactivenovelplatform.dto.response.UserResponseDto;
import project.interactivenovelplatform.entity.AppUserEntity;
import project.interactivenovelplatform.service.StorageService;
import project.interactivenovelplatform.service.UserService;

import java.security.Principal;

@RestController
@RequestMapping("/api/users")
@AllArgsConstructor
public class UserController {
    private final UserService userService;
    private final StorageService storageService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDto> getMyProfile(Authentication authentication) {
        var user = userService.findByUsername(authentication.getName());
        return ResponseEntity.ok().body(user);
    }

    @PostMapping("/me/avatar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDto> uploadUserAvatar(@RequestParam("file") MultipartFile file, Principal principal) {
        var user = userService.uploadUserAvatar(file, principal);
        return ResponseEntity.ok().body(user);
    }
    @PostMapping("/me/update")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserResponseDto> updateProfile(Authentication authentication ,@Valid @RequestBody UserUpdateRequestDto dto){
        var Principall= (AppUserEntity) authentication.getPrincipal();
        Long id = Principall.getId();
        var user = userService.updateProfileDetails(id, dto);
        return ResponseEntity.ok().body(user);
    }
    @PostMapping("/me/password")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> changePassword ( Authentication authentication, @Valid @RequestBody ChangePasswordRequestDto changePasswordRequestDto ){
        var Principall= (AppUserEntity) authentication.getPrincipal();
        Long id = Principall.getId();
        userService.changePassword(id, changePasswordRequestDto);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/search")
    public ResponseEntity<Page<UserResponseDto>> findByUsername(
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size){
        return ResponseEntity.ok().body(userService.searchUsers(page,size,username));
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
