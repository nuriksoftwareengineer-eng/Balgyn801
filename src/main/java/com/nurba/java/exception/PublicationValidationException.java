package com.nurba.java.exception;

import java.util.List;

public class PublicationValidationException extends RuntimeException {

    private final List<String> errors;

    public PublicationValidationException(List<String> errors) {
        super("Design cannot be published: " + String.join(", ", errors));
        this.errors = errors;
    }

    public List<String> getErrors() {
        return errors;
    }
}
