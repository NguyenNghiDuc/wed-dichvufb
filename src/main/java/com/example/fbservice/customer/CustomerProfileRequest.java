package com.example.fbservice.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CustomerProfileRequest(
    @NotBlank @Size(min=2, max=80) String displayName,
    @Size(max=20) String phone
) {}
