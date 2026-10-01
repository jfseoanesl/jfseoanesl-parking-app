package com.parking.presentation.controller;

import com.parking.domain.exception.BusinessRuleViolationException;
import com.parking.domain.model.VehicleType;
import com.parking.domain.repository.VehicleTypeRepository;
import com.parking.presentation.dto.CreateVehicleTypeRequest;
import com.parking.presentation.dto.VehicleTypeResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/vehicle-types")
public class VehicleTypeController {

    private final VehicleTypeRepository vehicleTypeRepository;

    public VehicleTypeController(VehicleTypeRepository vehicleTypeRepository) {
        this.vehicleTypeRepository = vehicleTypeRepository;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','EMPLOYEE')")
    public List<VehicleTypeResponse> list() {
        return vehicleTypeRepository.findAllActive().stream().map(VehicleTypeResponse::from).toList();
    }

    /** RF-017: la arquitectura permite nuevos tipos de vehículo sin cambios de código. */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public VehicleTypeResponse create(@Valid @RequestBody CreateVehicleTypeRequest request) {
        String code = request.code().trim().toUpperCase();
        if (vehicleTypeRepository.existsByCode(code)) {
            throw new BusinessRuleViolationException("VEHICLE_TYPE_CODE_TAKEN",
                    "Ya existe un tipo de vehículo con ese código");
        }
        VehicleType saved = vehicleTypeRepository.save(
                new VehicleType(UUID.randomUUID().toString(), code, request.name(), true));
        return VehicleTypeResponse.from(saved);
    }
}
