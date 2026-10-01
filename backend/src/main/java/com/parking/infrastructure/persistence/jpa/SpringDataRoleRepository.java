package com.parking.infrastructure.persistence.jpa;

import com.parking.infrastructure.persistence.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataRoleRepository extends JpaRepository<RoleEntity, String> {

    Optional<RoleEntity> findByCode(String code);
}
