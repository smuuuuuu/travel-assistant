package com.itbaizhan.travelmanager.security;

import com.itbaizhan.travelcommon.pojo.ManagerUser;
import com.itbaizhan.travelmanager.config.ManagerJwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class ManagerJwtService {

    private final ManagerJwtProperties properties;

    private SecretKey key() {
        byte[] bytes = properties.getSecret().getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 32) {
            throw new IllegalStateException("manager.jwt.secret must be at least 32 bytes for HS256");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    public String createToken(ManagerUser user) {
        long now = System.currentTimeMillis();
        long expMs = now + properties.getExpirationHours() * 3600_000L;
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("mid", user.getId())
                .claim("role", user.getRole() != null ? user.getRole() : "ROLE_ADMIN")
                .issuedAt(new Date(now))
                .expiration(new Date(expMs))
                .signWith(key())
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
