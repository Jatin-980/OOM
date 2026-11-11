package Banking_system.Code;

/**
 * Thrown when customer authentication, login verification, or account lockout checks fail.
 */
public class AuthenticationException extends BankingException {

    public AuthenticationException(String message) {
        super(message);
    }

    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}

