package com.example.employee.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UserPasswordUpdateRequest(
        @NotBlank @Size(min = 6, max = 72) String password
) {
}
