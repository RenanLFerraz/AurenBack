package com.renan.auren.controllers;

import com.renan.auren.dtos.FirebaseLoginRequest;
import com.renan.auren.dtos.LoginRequest;
import com.renan.auren.services.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request)
            throws Exception {

        return authService.login(request);
    }

    @PostMapping("/firebase-login")
    public ResponseEntity<?> firebaseLogin(@RequestBody FirebaseLoginRequest request)
            throws Exception {
        
        System.out.println("=== Firebase Login Request ===");
        System.out.println("Token recebido: " + (request.token() != null ? request.token().substring(0, Math.min(50, request.token().length())) + "..." : "NULL"));
        System.out.println("Token vazio: " + (request.token() == null || request.token().isEmpty()));

        return authService.firebaseLogin(request);
    }
}
