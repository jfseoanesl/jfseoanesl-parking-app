package com.parking.domain.repository;

import com.parking.domain.model.Role;

import java.util.Optional;

/** Puerto de dominio (Repository Pattern, 06-architecture.md). */
public interface RoleRepository {

    Optional<Role> findById(String id);

    Optional<Role> findByCode(String code);
}
