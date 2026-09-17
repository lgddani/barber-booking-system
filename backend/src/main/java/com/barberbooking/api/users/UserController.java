package com.barberbooking.api.users;

import com.barberbooking.api.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal UserPrincipal principal) {
        return new UserResponse(
            principal.getId(),
            principal.getUsername(),
            principal.user().getFullName(),
            principal.getRole()
        );
    }
}
