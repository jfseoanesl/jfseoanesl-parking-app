package com.parking.infrastructure.persistence.adapter;

import com.parking.domain.model.Role;
import com.parking.domain.repository.RoleRepository;
import com.parking.infrastructure.persistence.entity.RoleEntity;
import com.parking.infrastructure.persistence.jpa.SpringDataRoleRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class RoleRepositoryAdapter implements RoleRepository {

    private final SpringDataRoleRepository springDataRepository;

    public RoleRepositoryAdapter(SpringDataRoleRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<Role> findById(String id) {
        return springDataRepository.findById(id).map(RoleRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<Role> findByCode(String code) {
        return springDataRepository.findByCode(code).map(RoleRepositoryAdapter::toDomain);
    }

    static Role toDomain(RoleEntity entity) {
        return new Role(entity.getId(), entity.getCode(), entity.getName());
    }
}
