package com.ninelives.safepaws.exception;

import com.ninelives.safepaws.dto.ErrorResponse;

import java.util.List;

/**
 * A field breaks a rule that can only be checked in the service, e.g. a recipient who never wrote.
 * Returned as 400 "Some fields are invalid", like a validation error.
 */
public class InvalidRequestException extends RuntimeException {

    private final List<ErrorResponse.FieldError> fieldErrors;

    public InvalidRequestException(String field, String message) {
        super("Some fields are invalid");
        this.fieldErrors = List.of(new ErrorResponse.FieldError(field, message));
    }

    public List<ErrorResponse.FieldError> getFieldErrors() {
        return fieldErrors;
    }
}
