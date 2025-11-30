package com.renan.auren.controllers;

import com.renan.auren.domain.entities.User;
import com.renan.auren.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> createUser(@RequestBody User user)
            throws ExecutionException, InterruptedException {

        User created = userService.createUser(user);
        return ResponseEntity.ok(created);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id)
            throws ExecutionException, InterruptedException {

        User user = userService.getUserById(id);

        if (user == null) {
            return ResponseEntity.status(404).body("Usuário não encontrado");
        }

        return ResponseEntity.ok(user);
    }
}
