package com.aifa.modules.iam.application;

import com.aifa.modules.iam.application.dto.UpdateUserRequest;
import com.aifa.modules.iam.application.dto.UserProfileResponse;
import com.aifa.modules.iam.domain.User;
import com.aifa.modules.iam.infrastructure.UserRepository;
import com.aifa.shared.exception.ResourceNotFoundException;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public UserProfileResponse getProfile(UUID userId) {
        return toResponse(findUser(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateUserRequest request) {
        User user = findUser(userId);
        user.setAiMode(request.aiMode());
        user.setUpdatedAt(Instant.now());
        return toResponse(userRepository.save(user));
    }

    private User findUser(UUID userId) {
        return userRepository
                .findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserProfileResponse toResponse(User user) {
        return new UserProfileResponse(user.getId(), user.getEmail(), user.getAiMode());
    }
}
