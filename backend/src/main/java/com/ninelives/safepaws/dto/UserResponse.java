package com.ninelives.safepaws.dto;

import com.ninelives.safepaws.model.User;

/**
 * A user as the API shows it: never the password or its hash.
 */
public record UserResponse(
        Long id,
        String email,
        String displayName
) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName());
    }
}
