package app.bloom.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{9,14}$") String phoneNumber,
        @NotBlank @Size(max = 128) String password) {
}
