package com.aifa.modules.iam.infrastructure;

import com.aifa.modules.iam.application.UserService;
import com.aifa.modules.iam.application.dto.UpdateUserRequest;
import com.aifa.modules.iam.application.dto.UserProfileResponse;
import com.aifa.shared.security.CurrentUserProvider;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;
    private final CurrentUserProvider currentUserProvider;

    public UserController(UserService userService, CurrentUserProvider currentUserProvider) {
        this.userService = userService;
        this.currentUserProvider = currentUserProvider;
    }

    @GetMapping("/me")
    public UserProfileResponse getProfile() {
        return userService.getProfile(currentUserProvider.getCurrentUserId());
    }

    @PatchMapping("/me")
    public UserProfileResponse updateProfile(@Valid @RequestBody UpdateUserRequest request) {
        return userService.updateProfile(currentUserProvider.getCurrentUserId(), request);
    }
}
