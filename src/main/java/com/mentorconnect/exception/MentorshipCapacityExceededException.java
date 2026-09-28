package com.mentorconnect.exception;

public class MentorshipCapacityExceededException extends RuntimeException {
    public MentorshipCapacityExceededException(String message) {
        super(message);
    }
}
