package com.parking.application.auth;

import com.parking.infrastructure.security.ParkingUserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

/** RF-001 a RF-004. Orquesta la autenticacion delegando en Spring Security (ADR-004). */
@Service
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository securityContextRepository;
    private final LoginAttemptService loginAttemptService;

    public AuthService(AuthenticationManager authenticationManager,
                        SecurityContextRepository securityContextRepository,
                        LoginAttemptService loginAttemptService) {
        this.authenticationManager = authenticationManager;
        this.securityContextRepository = securityContextRepository;
        this.loginAttemptService = loginAttemptService;
    }

    /**
     * Autentica al usuario y persiste el contexto de seguridad en la sesion HTTP (cookie
     * httpOnly). Lanza BadCredentialsException/DisabledException si falla (RF-002), manejadas
     * de forma centralizada por GlobalExceptionHandler.
     */
    public ParkingUserPrincipal login(String username, String password,
                                       HttpServletRequest request, HttpServletResponse response) {
        // OWASP A07: limite de intentos por usuario (429 si esta bloqueado).
        loginAttemptService.checkNotBlocked(username);
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(username, password));
        } catch (AuthenticationException e) {
            loginAttemptService.recordFailure(username, clientAddress(request));
            throw e;
        }
        loginAttemptService.recordSuccess(username);

        // OWASP A07 (session fixation): si ya existia una sesion anonima (se crea al pedir el token
        // CSRF), se le cambia el ID al autenticar; asi un ID fijado de antemano no sirve.
        if (request.getSession(false) != null) {
            request.changeSessionId();
        }

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);
        securityContextRepository.saveContext(context, request, response);

        return (ParkingUserPrincipal) authentication.getPrincipal();
    }

    /** Solo para el log: detras de nginx, la IP real llega en X-Real-IP (informativa, falsificable). */
    private static String clientAddress(HttpServletRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        return realIp != null && !realIp.isBlank()
                ? realIp.replaceAll("[^0-9a-fA-F:.]", "") + " (vía proxy)"
                : request.getRemoteAddr();
    }
}
