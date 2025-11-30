package project.interactivenovelplatform.controller;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import project.interactivenovelplatform.dto.response.UserResponseDto;
import project.interactivenovelplatform.service.UserService;

import java.security.Key;

@RestController
@AllArgsConstructor
public class UserController {
    private final UserService userService;

    @GetMapping()
    public ResponseEntity<String> login(){
        return ResponseEntity.status(HttpStatus.OK).body("Привет");
    }
    @GetMapping("/users/me")
    @PreAuthorize("isAuthenticated()")
    public  UserResponseDto findByUsername(Authentication authentication){
        return userService.findByUsername(authentication.getName());
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
