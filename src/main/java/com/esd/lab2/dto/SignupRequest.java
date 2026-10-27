
package com.esd.lab2.dto;

import jakarta.validation.constraints.*;

public record SignupRequest(

        @Email
        @NotBlank
        String email,

        @NotBlank
        @Size(min = 8, max = 64)
        @Pattern(
                regexp = ".*\\d.*",
                message = "Password must contain at least one digit"
        )
        String password,

        @NotBlank
        @Size(max = 100)
        String fullName
) {}
