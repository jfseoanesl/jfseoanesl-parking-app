package com.parking.infrastructure.persistence.adapter;

import com.parking.domain.model.User;
import com.parking.domain.repository.UserRepository;
import com.parking.infrastructure.persistence.entity.UserEntity;
import com.parking.infrastructure.persistence.jpa.SpringDataUserRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class UserRepositoryAdapter implements UserRepository {

    private final SpringDataUserRepository springDataRepository;

    public UserRepositoryAdapter(SpringDataUserRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return springDataRepository.findByUsername(username).map(UserRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<User> findById(String id) {
        return springDataRepository.findById(id).map(UserRepositoryAdapter::toDomain);
    }

    @Override
    public List<User> findAll() {
        return springDataRepository.findAll().stream().map(UserRepositoryAdapter::toDomain).toList();
    }

    @Override
    public User save(User user) {
        UserEntity entity = new UserEntity(user.id(), user.username(), user.passwordHash(), user.fullName(),
                user.roleId(), user.active(), user.createdAt(), user.updatedAt());
        return toDomain(springDataRepository.save(entity));
    }

    @Override
    public boolean existsByUsername(String username) {
        return springDataRepository.existsByUsername(username);
    }

    static User toDomain(UserEntity entity) {
        return new User(
                entity.getId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                entity.getFullName(),
                entity.getRoleId(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
