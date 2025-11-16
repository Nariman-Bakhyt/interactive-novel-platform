package project.interactivenovelplatform.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
    public String main(
            Map<String,Object> model
    ){
        var uuser= userService.findAll();
        model.put("uuser",uuser);
        return "registration";
    }

    @PostMapping()
    public String addUser(
            @ModelAttribute @Valid RegistrationRequestDto registrationRequestDto,
            BindingResult bindingResult,
            Map<String, Object> model
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
