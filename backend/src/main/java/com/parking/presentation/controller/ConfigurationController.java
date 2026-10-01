package com.parking.presentation.controller;

import com.parking.application.configuration.ConfigurationService;
import com.parking.infrastructure.security.ParkingUserPrincipal;
import com.parking.presentation.dto.ConfigurationResponse;
import com.parking.presentation.dto.ConfigurationUpdateRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** UC-013 (RF-010 a RF-015). */
@RestController
@RequestMapping("/api/configuration")
public class ConfigurationController {

    private final ConfigurationService configurationService;

    public ConfigurationController(ConfigurationService configurationService) {
        this.configurationService = configurationService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public ConfigurationResponse get() {
        return ConfigurationResponse.from(configurationService.get());
    }

    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ConfigurationResponse update(@Valid @RequestBody ConfigurationUpdateRequest request,
                                         @AuthenticationPrincipal ParkingUserPrincipal principal) {
        var input = new ConfigurationService.ConfigurationInput(request.parkingName(), request.nit(),
                request.address(), request.phone(), request.maxCapacity(), request.timezone(), request.currency(),
                request.receiptFormatJson());
        return ConfigurationResponse.from(configurationService.update(input, principal.id()));
    }
}
