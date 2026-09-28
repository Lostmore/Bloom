package app.bloom.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Pattern(regexp = "^\\+[1-9]\\d{9,14}$") String phoneNumber,
        @NotBlank @Size(min = 12, max = 128) String password
) {
}
