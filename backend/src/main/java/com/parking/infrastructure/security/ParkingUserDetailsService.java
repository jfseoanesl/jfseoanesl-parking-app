package com.parking.infrastructure.security;

import com.parking.domain.model.Role;
import com.parking.domain.model.User;
import com.parking.domain.repository.RoleRepository;
import com.parking.domain.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class ParkingUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public ParkingUserDetailsService(UserRepository userRepository, RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario o contraseña inválidos"));
        Role role = roleRepository.findById(user.roleId())
                .orElseThrow(() -> new IllegalStateException("Usuario sin rol asignado: " + username));
        return new ParkingUserPrincipal(
                user.id(), user.username(), user.passwordHash(), user.fullName(), role.code(), user.active());
    }
}
