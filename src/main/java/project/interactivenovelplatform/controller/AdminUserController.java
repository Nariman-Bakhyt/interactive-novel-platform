package project.interactivenovelplatform.controller;

import lombok.AllArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import project.interactivenovelplatform.dto.response.UserResponseDto;
import project.interactivenovelplatform.service.AdminUserService;



@RestController
@AllArgsConstructor
public class AdminUserController {
    private final AdminUserService adminUserService;

    @GetMapping("/admin/users/{id}")
    public UserResponseDto findById(@PathVariable Long id){
        return adminUserService.findById(id);
    }

    @GetMapping("/admin/users/all")
    public Page<UserResponseDto> findAllUsers(
            @PageableDefault(size = 10) Pageable pageable
    ){
        return adminUserService.findAll(pageable);
    }
}
