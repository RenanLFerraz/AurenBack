package com.renan.auren.dtos;

import jakarta.validation.constraints.NotBlank;

public record FirebaseLoginRequest(
        @NotBlank
        String token
) {
}
