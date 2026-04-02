package com.itbaizhan.travelmanager.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.NonNull;
import com.itbaizhan.travelcommon.pojo.ManagerUser;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class ManagerJwtAuthenticationFilter extends OncePerRequestFilter {

    private final ManagerJwtService managerJwtService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String raw = header.substring(7).trim();
            if (StringUtils.hasText(raw)) {
                try {
                    Claims claims = managerJwtService.parse(raw);
                    String username = claims.getSubject();
                    String role = claims.get("role", String.class);
                    if (!StringUtils.hasText(role)) {
                        role = "ROLE_ADMIN";
                    }
                    Long mid = null;
                    Object midClaim = claims.get("mid");
                    if (midClaim instanceof Number n) {
                        mid = n.longValue();
                    }
                    ManagerUser minimal = new ManagerUser();
                    minimal.setId(mid);
                    minimal.setUsername(username);
                    minimal.setRole(role);
                    var principal = new ManagerPrincipal(minimal);
                    var auth = new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority(role)));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                } catch (Exception ignored) {
                    SecurityContextHolder.clearContext();
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
