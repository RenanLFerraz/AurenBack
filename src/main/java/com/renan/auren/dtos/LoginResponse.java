package com.renan.auren.dtos;

import com.renan.auren.domain.entities.User;

public record LoginResponse(
        User user,
        String token
) {}
