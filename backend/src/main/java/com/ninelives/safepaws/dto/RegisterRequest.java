package com.ninelives.safepaws.dto;

import com.ninelives.safepaws.validation.MaxUtf8Bytes;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.Locale;

/**
 * Body of POST /auth/register. The email is trimmed and lowercased and the display name trimmed
 * before validation, so " Ana@SafePaws.dev " is checked and saved as "ana@safepaws.dev".
 */
public record RegisterRequest(

        @NotBlank(message = "Email is required")
        @Size(max = 255, message = "Email must be at most 255 characters")
        // empty passes, so a blank email only gets "Email is required"
        @Email(regexp = "^$|^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$", message = "Email must be a valid email address")
        String email,

        // Blank passes the pattern, so an empty password only gets "Password is required".
        // A special character is anything that is not a letter or a digit.
        @NotBlank(message = "Password is required")
        @Pattern(regexp = "^\\s*$|^(?=.*\\p{Lu})(?=.*\\p{Nd})(?=.*[^\\p{L}\\p{Nd}]).{8,}$",
                message = "Password must have at least 8 characters, one uppercase letter, one digit "
                        + "and one special character")
        @MaxUtf8Bytes(value = 72, message = "Password must be at most 72 characters")
        String password,

        @NotBlank(message = "Display name is required")
        @Size(max = 50, message = "Display name must be at most 50 characters")
        String displayName
) {

    public RegisterRequest {
        email = email == null ? null : email.strip().toLowerCase(Locale.ROOT);
        displayName = displayName == null ? null : displayName.strip();
    }

    @Override
    public String toString() {
        // never log the password
        return "RegisterRequest[email=" + email + ", displayName=" + displayName + "]";
    }
}
