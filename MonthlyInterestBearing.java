package Banking_system.Code;

/**
 * Interface Segregation Principle (ISP) & Open/Closed Principle (OCP):
 * Represents an account that participates in the monthly interest cycle.
 * Only accounts that calculate periodic monthly interest (such as SavingsAccount
 * and Loan) implement this interface, keeping other account types (Current, FixedDeposit)
 * free from unnecessary interest-cycle methods.
 */
public interface MonthlyInterestBearing {

    /**
     * Determines whether the account has already had monthly interest processed
     * during the current calendar month.
     *
     * @return true if interest has already been credited or debited this month; false otherwise.
     */
    boolean hasReceivedInterestThisMonth();

    /**
     * Executes the monthly interest cycle for this account (crediting profit or debiting interest),
     * records the corresponding transaction, updates the balance, and returns a formatted summary.
     *
     * @return Formatted summary string of the transaction and balances, or empty/null if no interest applied.
     */
    String processMonthlyInterestCycle();
}

