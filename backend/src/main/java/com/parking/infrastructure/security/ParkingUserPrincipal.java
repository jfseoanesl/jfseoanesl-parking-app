package com.parking.infrastructure.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.List;

/**
 * Principal de Spring Security que transporta los datos del usuario autenticado sin necesitar
 * una consulta adicional en cada request (id, nombre completo, codigo de rol).
 */
public class ParkingUserPrincipal implements UserDetails {

    private final String id;
    private final String username;
    private final String passwordHash;
    private final String fullName;
    private final String roleCode;
    private final boolean active;

    public ParkingUserPrincipal(String id, String username, String passwordHash, String fullName,
                                 String roleCode, boolean active) {
        this.id = id;
        this.username = username;
        this.passwordHash = passwordHash;
        this.fullName = fullName;
        this.roleCode = roleCode;
        this.active = active;
    }

    public String id() {
        return id;
    }

    public String fullName() {
        return fullName;
    }

    public String roleCode() {
        return roleCode;
    }

    @Override
    public List<GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + roleCode));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
