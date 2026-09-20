package app.bloom.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        // Пример базовой валидации номера: от 10 до 15 цифр
        @NotBlank @Pattern(regexp = "^\\+?[1-9]\\d{1,14}$", message = "Invalid phone number format")
        String phoneNumber,

        @NotBlank @Size(min = 4, max = 128)
        String password
) { }
