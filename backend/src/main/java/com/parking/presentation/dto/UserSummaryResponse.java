package com.parking.presentation.dto;

import com.parking.infrastructure.security.ParkingUserPrincipal;

/** Ver 08-api-contract.md / openapi.yaml #UserSummary. */
public record UserSummaryResponse(String id, String username, String fullName, String role) {

    public static UserSummaryResponse from(ParkingUserPrincipal principal) {
        return new UserSummaryResponse(
                principal.id(), principal.getUsername(), principal.fullName(), principal.roleCode());
    }
}
