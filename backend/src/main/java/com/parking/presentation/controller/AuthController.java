package com.parking.presentation.controller;

import com.parking.application.auth.AuthService;
import com.parking.infrastructure.security.ParkingUserPrincipal;
import com.parking.presentation.dto.LoginRequest;
import com.parking.presentation.dto.UserSummaryResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** UC-001. Ver 08-api-contract.md §Autenticación. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public UserSummaryResponse login(@Valid @RequestBody LoginRequest request,
                                      HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        ParkingUserPrincipal principal = authService.login(
                request.username(), request.password(), httpRequest, httpResponse);
        return UserSummaryResponse.from(principal);
    }

    @GetMapping("/me")
    public UserSummaryResponse me(@AuthenticationPrincipal ParkingUserPrincipal principal) {
        return UserSummaryResponse.from(principal);
    }
}
