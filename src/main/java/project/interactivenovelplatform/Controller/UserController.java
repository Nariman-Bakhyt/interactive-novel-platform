package project.interactivenovelplatform.Controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import project.interactivenovelplatform.Entity.AppUser;
import project.interactivenovelplatform.Entity.Role;
import project.interactivenovelplatform.Entity.RoleEntity;
import project.interactivenovelplatform.repository.RoleRepository;
import project.interactivenovelplatform.repository.UserRepository;

import java.util.Map;
import java.util.Optional;

@Controller()
public class UserController {
    @Autowired
    private RoleRepository roleRepository;
    @Autowired
    private UserRepository userRepository;

    public UserController(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @GetMapping()
    public String main(
            Map<String,Object> model
    ){
        var uuser= userRepository.findAll();
        model.put("uuser",uuser);
        return "registration";
    }

    @PostMapping()
    public String addUser(
            @RequestParam String name,
            @RequestParam String password ,
            @RequestParam String email,
            Map<String, Object> model
    ){
        Optional<RoleEntity> roleOptional = roleRepository.findByName(Role.USER);
        if (roleOptional.isEmpty()) {
            model.put("error","Role does not exist");
            return "registration";
        }
        RoleEntity role = roleOptional.get();
        AppUser user =new AppUser(
                name,
                password,
                email,
                role
        );

        var newUser = userRepository.save(user);

        if (newUser.getId() == null) {
            model.put("error", "Непредвиденная ошибка при сохранении пользователя.");
        }
        var uuser= userRepository.findAll();
        model.put("uuser", uuser);
        return "registration";
    }
}
