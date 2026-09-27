package auca.ac.rw.exception;

/**
 * The Class InvalidCredentialsException.
 * Thrown when a login attempt fails — unknown loginId or wrong password.
 * Mapped to HTTP 401 by GlobalExceptionHandler.
 *
 * @version 1.0
 */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
