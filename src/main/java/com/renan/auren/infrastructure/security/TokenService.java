package com.renan.auren.infrastructure.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Service
public class TokenService {

    private final SecretKey secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // Gera token JWT da API
    public String generateToken(String email, Long userId) {

        Instant now = Instant.now();
        Instant expiration = now.plus(10, ChronoUnit.MINUTES);

        return Jwts.builder()
                .setSubject(email)
                .claim("id", userId)
                .setIssuedAt(new Date())
                .setExpiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    // Valida o token
    public Claims validateToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String getEmailFromToken(String token) {
        return validateToken(token).getSubject();
    }

    public Long getUserIdFromToken(String token) {
        return validateToken(token).get("id", Long.class);
    }

}
