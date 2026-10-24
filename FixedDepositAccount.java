package Banking_system.Code;

import java.time.LocalDate;

public class FixedDepositAccount extends Account {

    private int linkedSavingsAccountNo;
    private int tenureMonths;
    private double interestRate;
    private static final double MINIMUM_BALANCE = 1000.0;
    
    public FixedDepositAccount(int accountNo, int customerId, double balance, AccountStatus status, LocalDate opened, int linkedSavingsAccountNo, int tenureMonths) {
        super(accountNo, customerId, balance, status, opened);
        this.linkedSavingsAccountNo = linkedSavingsAccountNo;
        this.tenureMonths = tenureMonths;
        this.interestRate = determineInterestRate(tenureMonths);
    }
    
    // Helper to assign the correct slab interest rate[cite: 2]
    private double determineInterestRate(int months) {
        switch (months) {
            case 6: return 5.5;
            case 12: return 6.5;
            case 24: return 7.0;
            case 36: return 7.5;
            default: throw new IllegalArgumentException("Invalid FD tenure. Allowed: 6, 12, 24, 36 months.");
        }
    }
    
    @Override
    public AccountType getAccountType() {
        return AccountType.FIXED_DEPOSIT;
    }
    
    @Override
    public boolean deposit(double amount) {
        // FR-8: A Fixed Deposit never accepts a deposit after opening[cite: 2]
        recordTransaction("Deposit", amount, -1, "Failed", "Fixed Deposits do not accept deposits after opening");
        return false;
    }
    
    @Override
    public boolean withdraw(double amount) {
        // FR-7: No partial withdrawal before maturity[cite: 1]
        recordTransaction("Withdrawal", amount, -1, "Failed", "Fixed Deposits do not allow standard withdrawals");
        return false;
    }
    @Override
    public boolean transferOut(double amount, int destAccountNo) {
        recordTransaction("Transfer-out", amount, destAccountNo, "Failed", "Fixed Deposits do not allow outgoing transfers");
        return false;
    }

    @Override
    public boolean transferIn(double amount, int sourceAccountNo) {
        recordTransaction("Transfer-in", amount, this.getAccountNo(), "Failed", "Fixed Deposits do not accept incoming transfers");
        return false;
    }
    // Getters for FD specific fields

    public static double getMinimumBalance() {
        return MINIMUM_BALANCE;
    }
    public int getLinkedSavingsAccountNo() {
        return linkedSavingsAccountNo;
    }
    
    public int getTenureMonths() {
        return tenureMonths;
    }
    
    public double getInterestRate() {
        return interestRate;
    }
}