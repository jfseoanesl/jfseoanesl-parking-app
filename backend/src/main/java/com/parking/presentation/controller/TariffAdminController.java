package com.parking.presentation.controller;

import com.parking.application.tariff.TariffAdminService;
import com.parking.domain.model.Tariff;
import com.parking.infrastructure.security.ParkingUserPrincipal;
import com.parking.presentation.dto.TariffAdminRequest;
import com.parking.presentation.dto.TariffAdminResponse;
import com.parking.presentation.dto.UpdateActiveStatusRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** UC-008 (RF-018 a RF-022). Ver 08-api-contract.md §Tarifas. */
@RestController
@RequestMapping("/api/tariffs")
@PreAuthorize("hasRole('ADMIN')")
public class TariffAdminController {

    private final TariffAdminService tariffAdminService;

    public TariffAdminController(TariffAdminService tariffAdminService) {
        this.tariffAdminService = tariffAdminService;
    }

    @GetMapping
    public List<TariffAdminResponse> list(@RequestParam(required = false) String vehicleTypeId,
                                           @RequestParam(required = false) Boolean active) {
        return tariffAdminService.list(vehicleTypeId, active).stream().map(TariffAdminResponse::from).toList();
    }

    @GetMapping("/{id}")
    public TariffAdminResponse getById(@PathVariable String id) {
        return TariffAdminResponse.from(tariffAdminService.getById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TariffAdminResponse create(@Valid @RequestBody TariffAdminRequest request,
                                       @AuthenticationPrincipal ParkingUserPrincipal principal) {
        Tariff tariff = tariffAdminService.create(toInput(request), principal.id());
        return TariffAdminResponse.from(tariff);
    }

    @PutMapping("/{id}")
    public TariffAdminResponse update(@PathVariable String id, @Valid @RequestBody TariffAdminRequest request,
                                       @AuthenticationPrincipal ParkingUserPrincipal principal) {
        Tariff tariff = tariffAdminService.update(id, toInput(request), principal.id());
        return TariffAdminResponse.from(tariff);
    }

    @PatchMapping("/{id}/status")
    public TariffAdminResponse updateStatus(@PathVariable String id, @Valid @RequestBody UpdateActiveStatusRequest request,
                                             @AuthenticationPrincipal ParkingUserPrincipal principal) {
        Tariff tariff = tariffAdminService.setActive(id, request.active(), principal.id());
        return TariffAdminResponse.from(tariff);
    }

    private static TariffAdminService.TariffInput toInput(TariffAdminRequest r) {
        return new TariffAdminService.TariffInput(r.name(), r.description(), r.vehicleTypeId(), r.modality(),
                r.value(), r.firstFractionValue(), r.startTime(), r.endTime(), r.toleranceMinutes(), r.minTimeMinutes(), r.timeUnit(),
                r.fractionMinutes(), r.rounding(), r.priority(), r.active(), r.validFrom(), r.validTo());
    }
}
