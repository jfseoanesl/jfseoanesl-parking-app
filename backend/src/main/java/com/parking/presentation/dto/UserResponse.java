package com.parking.presentation.dto;

public record UserResponse(String id, String username, String fullName, String role, boolean active) {
}
