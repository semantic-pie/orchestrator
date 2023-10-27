package io.github.semanticpie.orchestrator.services.impl;

public class TrackServiceException extends RuntimeException {
    public TrackServiceException() {
    }

    public TrackServiceException(String message) {
        super(message);
    }

    public TrackServiceException(String message, Throwable cause) {
        super(message, cause);
    }

    public TrackServiceException(Throwable cause) {
        super(cause);
    }
}

