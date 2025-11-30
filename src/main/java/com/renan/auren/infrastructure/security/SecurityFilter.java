package com.renan.auren.infrastructure.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class SecurityFilter extends HttpFilter {

    @Autowired
    private TokenService tokenService;

    @Override
    protected void doFilter(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        String auth = req.getHeader("Authorization");

        if (auth == null || !auth.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }

        String token = auth.substring(7);

        try {
            Claims claims = tokenService.validateToken(token);
            req.setAttribute("userEmail", claims.getSubject());
            req.setAttribute("userId", claims.get("id", Long.class));
        } catch (Exception e) {
            res.setStatus(401);
            return;
        }

        chain.doFilter(req, res);
    }
}
