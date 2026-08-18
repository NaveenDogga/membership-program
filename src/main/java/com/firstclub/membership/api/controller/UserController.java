package com.firstclub.membership.api.controller;

import com.firstclub.membership.common.exception.ResourceNotFoundException;
import com.firstclub.membership.domain.model.User;
import com.firstclub.membership.domain.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Demo user management")
public class UserController {

    private final UserRepository userRepository;

    public record UserResponse(Long id, String name, String email, Set<String> cohorts) {
    }

    public record CreateUserRequest(@NotBlank String name, @NotBlank @Email String email, Set<String> cohorts) {
    }

    @GetMapping
    @Operation(summary = "List users")
    public ResponseEntity<List<UserResponse>> list() {
        return ResponseEntity.ok(userRepository.findAll().stream()
                .map(user -> new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCohorts()))
                .toList());
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Fetch a user")
    public ResponseEntity<UserResponse> get(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        return ResponseEntity.ok(new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCohorts()));
    }

    @PostMapping
    @Operation(summary = "Create a user")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        User user = userRepository.save(User.builder()
                .name(request.name())
                .email(request.email())
                .cohorts(request.cohorts() == null ? Set.of() : request.cohorts())
                .build());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UserResponse(user.getId(), user.getName(), user.getEmail(), user.getCohorts()));
    }
}
