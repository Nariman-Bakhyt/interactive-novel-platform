package project.interactivenovelplatform.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import project.interactivenovelplatform.dto.request.RegistrationRequestDto;
import project.interactivenovelplatform.repository.RoleRepository;
import project.interactivenovelplatform.service.UserService;

import java.util.Map;

@Controller()
public class UserController {
    private final UserService userService;
    @Autowired
    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping()
    public String login(Map<String, Object> model){
        return "main";
    }

    @GetMapping("/login")
    public String logout(Map<String, Object> model, CsrfToken csrfToken){
        return "login";
    }
    @GetMapping("/reg")
    public String regStart(
            Map<String, Object> model, CsrfToken csrfToken
    ){
        return "reg";
    }
    @PostMapping("/reg")
    public String regEnd(
            @ModelAttribute @Valid RegistrationRequestDto registrationRequestDto,
            BindingResult bindingResult,
            Map<String, Object> model, CsrfToken csrfToken
    ){
        if (bindingResult.hasErrors()) {
            // Добавляем ошибки в модель
            model.put("errors", bindingResult.getAllErrors());
            // Возвращаем ту же форму, чтобы пользователь исправил ошибки
            var uuser= userService.findAll();
            model.put("uuser", uuser);
            return "registration";
        }
        userService.registerUser(registrationRequestDto);
        return "reg";
    }

    @GetMapping("/main")
    public String main(
            Map<String,Object> model, CsrfToken csrfToken
    ){
        var uuser= userService.findAll();
        model.put("uuser",uuser);
        return "registration";
    }

    @PostMapping("/main")
    public String addUser(
            @ModelAttribute @Valid RegistrationRequestDto registrationRequestDto,
            BindingResult bindingResult,
            Map<String, Object> model, CsrfToken csrfToken
    ){
        if (bindingResult.hasErrors()) {
            // Добавляем ошибки в модель
            model.put("errors", bindingResult.getAllErrors());
            // Возвращаем ту же форму, чтобы пользователь исправил ошибки
            var uuser= userService.findAll();
            model.put("uuser", uuser);
            return "registration";
        }
        userService.registerUser(registrationRequestDto);

        var uuser= userService.findAll();
        model.put("uuser", uuser);
        return "registration";
    }

}
