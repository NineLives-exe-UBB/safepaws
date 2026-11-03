package com.ninelives.safepaws.model;

public enum Species {
    CAT("Cat"),
    DOG("Dog"),
    OTHER("Other");

    private final String displayName;

    Species(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
