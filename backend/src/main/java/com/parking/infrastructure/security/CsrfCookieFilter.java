package com.parking.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Spring Security 6+/7 difiere la escritura de la cookie XSRF-TOKEN hasta que algo lea
 * CsrfToken#getToken() explicitamente (para evitar exponerla quando no hace falta). Para una
 * SPA que necesita leer la cookie ANTES de su primer POST, hay que forzar esa lectura en cada
 * request - patron documentado oficialmente por Spring Security para "CSRF con SPA".
 */
public class CsrfCookieFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {
        CsrfToken csrfToken = (CsrfToken) request.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            csrfToken.getToken();
        }
        filterChain.doFilter(request, response);
    }
}
