package auca.ac.rw.exception;

/**
 * The Class DuplicateResourceException.
 * Thrown when creating/updating a record would clash with a unique value
 * another record already has (e.g. an existing course code).
 * Mapped to HTTP 409 by GlobalExceptionHandler.
 *
 * @version 1.0
 */
public class DuplicateResourceException extends RuntimeException {
    public DuplicateResourceException(String message) {
        super(message);
    }
}
