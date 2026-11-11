package Banking_system.Code;

/**
 * Base custom checked exception for domain-level banking errors.
 * Demonstrates user-defined exception hierarchies for Object-Oriented Methodology.
 */
public class BankingException extends Exception {

    public BankingException(String message) {
        super(message);
    }

    public BankingException(String message, Throwable cause) {
        super(message, cause);
    }
}

