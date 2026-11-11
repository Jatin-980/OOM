package Banking_system.Code;

/**
 * Thrown when financial transaction pre-conditions or business rules are violated.
 */
public class InvalidTransactionException extends BankingException {

    public InvalidTransactionException(String message) {
        super(message);
    }

    public InvalidTransactionException(String message, Throwable cause) {
        super(message, cause);
    }
}

