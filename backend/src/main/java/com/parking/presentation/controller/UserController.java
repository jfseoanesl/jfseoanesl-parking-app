package com.parking.presentation.controller;

import com.parking.application.user.UserService;
import com.parking.domain.model.Role;
import com.parking.domain.model.User;
import com.parking.domain.repository.RoleRepository;
import com.parking.infrastructure.security.ParkingUserPrincipal;
import com.parking.presentation.dto.ChangeOwnPasswordRequest;
import com.parking.presentation.dto.ChangePasswordRequest;
import com.parking.presentation.dto.CreateUserRequest;
import com.parking.presentation.dto.UpdateActiveStatusRequest;
import com.parking.presentation.dto.UserResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** UC-009 (RF-005 a RF-009). Ver 08-api-contract.md §Usuarios. */
@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final RoleRepository roleRepository;

    public UserController(UserService userService, RoleRepository roleRepository) {
        this.userService = userService;
        this.roleRepository = roleRepository;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> list() {
        return userService.listAll().stream().map(this::toResponse).toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request,
                                @AuthenticationPrincipal ParkingUserPrincipal principal) {
        User user = userService.create(request.username(), request.fullName(), request.password(),
                request.role(), principal.id());
        return toResponse(user);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateStatus(@PathVariable String id, @Valid @RequestBody UpdateActiveStatusRequest request,
                                      @AuthenticationPrincipal ParkingUserPrincipal principal) {
        User user = userService.setActive(id, request.active(), principal.id());
        return toResponse(user);
    }

    @PutMapping("/{id}/password")
    @PreAuthorize("hasRole('ADMIN')")
    public void changeOtherPassword(@PathVariable String id, @Valid @RequestBody ChangePasswordRequest request,
                                     @AuthenticationPrincipal ParkingUserPrincipal principal) {
        userService.changePassword(id, request.newPassword(), principal.id(), true);
    }

    @PutMapping("/me/password")
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public void changeOwnPassword(@Valid @RequestBody ChangeOwnPasswordRequest request,
                                   @AuthenticationPrincipal ParkingUserPrincipal principal) {
        userService.changeOwnPassword(principal.id(), request.currentPassword(), request.newPassword());
    }

    private UserResponse toResponse(User user) {
        String roleCode = roleRepository.findById(user.roleId()).map(Role::code).orElse("UNKNOWN");
        return new UserResponse(user.id(), user.username(), user.fullName(), roleCode, user.active());
    }
}
