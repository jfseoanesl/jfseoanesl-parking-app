package com.parking.infrastructure.security;

import com.parking.domain.model.User;
import com.parking.domain.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Revisa en cada peticion que el usuario de la sesion siga existiendo y activo (revision OWASP A01/A07,
 * 2026-09-29). Antes, desactivar a un usuario no afectaba a su sesion ya abierta: podia seguir
 * operando (se comprobo registrando un ingreso). Si fue desactivado, se invalida su sesion y se limpia
 * el contexto: la peticion continua como anonima y la regla "anyRequest().authenticated()" responde 401.
 * Cuesta una consulta por id por peticion, aceptable para el volumen de un parqueadero.
 */
public class ActiveUserSessionFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(ActiveUserSessionFilter.class);

    private final UserRepository userRepository;

    public ActiveUserSessionFilter(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof ParkingUserPrincipal principal) {
            Optional<User> user = userRepository.findById(principal.id());
            if (user.isEmpty() || !user.get().active()) {
                log.warn("Sesión revocada: el usuario '{}' fue desactivado o eliminado", principal.getUsername());
                SecurityContextHolder.clearContext();
                HttpSession session = request.getSession(false);
                if (session != null) {
                    session.invalidate();
                }
            }
        }
        chain.doFilter(request, response);
    }
}
