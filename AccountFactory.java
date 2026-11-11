package Banking_system.Code;

import java.time.LocalDate;
import Banking_system.Code.Account.AccountStatus;
import Banking_system.Code.Account.AccountType;

/**
 * AccountFactory implements the Factory Creational Design Pattern.
 * It encapsulates the instantiation logic for all Account subclasses
 * (SavingsAccount, CurrentAccount, FixedDepositAccount, and Loan).
 */
public class AccountFactory {

    // Prevent direct instantiation of factory utility class
    private AccountFactory() {}

    // ==================== 1. NEW ACCOUNT CREATION (for UI / OperatorMenu) ====================

    /**
     * Creates a new SavingsAccount with a temporary unassigned account number (-1).
     */
    public static SavingsAccount createNewSavingsAccount(int customerId, double initialDeposit,
                                                         AccountStatus status, LocalDate opened) {
        return createSavingsAccount(-1, customerId, initialDeposit, status, opened);
    }

    /**
     * Creates a new CurrentAccount with a temporary unassigned account number (-1).
     */
    public static CurrentAccount createNewCurrentAccount(int customerId, double initialDeposit,
                                                         AccountStatus status, LocalDate opened) {
        return createCurrentAccount(-1, customerId, initialDeposit, status, opened);
    }

    /**
     * Creates a new FixedDepositAccount with a temporary unassigned account number (-1).
     */
    public static FixedDepositAccount createNewFixedDepositAccount(int customerId, double initialDeposit,
                                                                   AccountStatus status, LocalDate opened,
                                                                   int linkedSavingsAccountNo, int tenureMonths) {
        return createFixedDepositAccount(-1, customerId, initialDeposit, status, opened, linkedSavingsAccountNo, tenureMonths);
    }

    /**
     * Creates a new Loan account with a temporary unassigned account number (-1).
     */
    public static Loan createNewLoan(int customerId, double principalAmount,
                                     AccountStatus status, LocalDate opened,
                                     double interestRate, int loanDurationInMonths) {
        return createLoan(-1, customerId, principalAmount, status, opened, principalAmount, interestRate, loanDurationInMonths);
    }

    // ==================== 2. HYDRATION / STORAGE (for BankStorage / known accountNo) ====================

    /**
     * Hydrates an existing SavingsAccount with a known account number.
     */
    public static SavingsAccount createSavingsAccount(int accountNo, int customerId, double balance,
                                                      AccountStatus status, LocalDate opened) {
        return new SavingsAccount(accountNo, customerId, balance, status, opened);
    }

    /**
     * Hydrates an existing CurrentAccount with a known account number.
     */
    public static CurrentAccount createCurrentAccount(int accountNo, int customerId, double balance,
                                                      AccountStatus status, LocalDate opened) {
        return new CurrentAccount(accountNo, customerId, balance, status, opened);
    }

    /**
     * Hydrates an existing FixedDepositAccount with a known account number.
     */
    public static FixedDepositAccount createFixedDepositAccount(int accountNo, int customerId, double balance,
                                                                AccountStatus status, LocalDate opened,
                                                                int linkedSavingsAccountNo, int tenureMonths) {
        return new FixedDepositAccount(accountNo, customerId, balance, status, opened, linkedSavingsAccountNo, tenureMonths);
    }

    /**
     * Hydrates an existing Loan account with a known account number.
     */
    public static Loan createLoan(int accountNo, int customerId, double balance,
                                  AccountStatus status, LocalDate opened,
                                  double principalAmount, double interestRate, int loanDurationInMonths) {
        return new Loan(accountNo, customerId, balance, status, opened, principalAmount, interestRate, loanDurationInMonths);
    }
}

