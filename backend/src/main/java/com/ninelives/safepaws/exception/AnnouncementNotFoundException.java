package com.ninelives.safepaws.exception;

public class AnnouncementNotFoundException extends RuntimeException {

    public AnnouncementNotFoundException(Long id) {
        super("Announcement not found with id: " + id);
    }
}
