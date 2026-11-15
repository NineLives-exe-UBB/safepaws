package com.ninelives.safepaws.exception;

public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException() {
        super("Authentication is required");
    }
}