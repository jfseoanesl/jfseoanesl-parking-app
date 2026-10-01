package com.parking.domain.repository;

import com.parking.domain.model.User;

import java.util.List;
import java.util.Optional;

/**
 * Puerto de dominio (Repository Pattern, 06-architecture.md). No depende de JPA ni de ningun
 * framework; lo implementa un adaptador en infrastructure.persistence.adapter.
 */
public interface UserRepository {

    Optional<User> findByUsername(String username);

    Optional<User> findById(String id);

    List<User> findAll();

    User save(User user);

    boolean existsByUsername(String username);
}
