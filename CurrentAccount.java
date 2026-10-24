package Banking_system.Code;

import java.time.LocalDate;

public class CurrentAccount extends Account {

    private static final double OVERDRAFT_LIMIT = -10000.0;
    private static final double OVERDRAFT_FEE = 100.0;
    private static final double DAILY_LIMIT = 50000.0;
    private static final double MINIMUM_BALANCE = 5000.0;

    
    public CurrentAccount(int accountNo, int customerId, double balance, AccountStatus status, LocalDate opened) {
        super(accountNo, customerId, balance, status, opened);
    }
    
    @Override
    public AccountType getAccountType() {
        return AccountType.CURRENT;
    }
    
    @Override
    public boolean deposit(double amount) {
        if (amount <= 0) {
            recordTransaction("Deposit", amount, -1, "Failed", "Amount must be positive");
            return false;
        }
        
        setBalance(getBalance() + amount);
        recordTransaction("Deposit", amount, -1, "Success", "None");
        return true;
    }
    
    @Override
    public boolean withdraw(double amount) {
        if (amount <= 0) {
            recordTransaction("Withdrawal", amount, -1, "Failed", "Amount must be positive");
            return false;
        }
        
        // Check if this withdrawal pushes a zero/positive balance into the negative
        boolean feeApplies = (getBalance() >= 0) && ((getBalance() - amount) < 0);
        double totalDeduction = feeApplies ? (amount + OVERDRAFT_FEE) : amount;
        
        // FR-10: Limit overdraft to Rs. 10,000 below zero
        if (getBalance() - totalDeduction < OVERDRAFT_LIMIT) {
            recordTransaction("Withdrawal", amount, -1, "Failed", "Exceeds Rs. 10,000 overdraft limit");
            return false;
        }
        
        // FR-11: Reject if daily limit exceeded[cite: 2]
        if (getTodayOutwardTotal() + amount > DAILY_LIMIT) {
            recordTransaction("Withdrawal", amount, -1, "Failed", "Daily withdrawal limit of Rs. 50,000 exceeded");
            return false;
        }
        
        // Process the standard withdrawal
        setBalance(getBalance() - amount);
        recordTransaction("Withdrawal", amount, -1, "Success", "None");
        
        // Apply the overdraft fee automatically if it triggered[cite: 2]
        if (feeApplies) {
            setBalance(getBalance() - OVERDRAFT_FEE);
            recordTransaction("Fee", OVERDRAFT_FEE, -1, "Success", "Overdraft crossing fee applied");
        }
        
        return true;
    }
    @Override
    public boolean transferOut(double amount, int destAccountNo) {
        if (amount <= 0) {
            recordTransaction("Transfer-out", amount, destAccountNo, "Failed", "Amount must be positive");
            return false;
        }

        boolean feeApplies = (getBalance() >= 0) && ((getBalance() - amount) < 0);
        double totalDeduction = feeApplies ? (amount + OVERDRAFT_FEE) : amount;

        if (getBalance() - totalDeduction < OVERDRAFT_LIMIT) {
            recordTransaction("Transfer-out", amount, destAccountNo, "Failed", "Exceeds Rs. 10,000 overdraft limit");
            return false;
        }
        if (getTodayOutwardTotal() + amount > DAILY_LIMIT) {
            recordTransaction("Transfer-out", amount, destAccountNo, "Failed", "Daily outward limit of Rs. 50,000 exceeded");
            return false;
        }

        setBalance(getBalance() - amount);
        recordTransaction("Transfer-out", amount, destAccountNo, "Success", "None");

        if (feeApplies) {
            setBalance(getBalance() - OVERDRAFT_FEE);
            recordTransaction("Fee", OVERDRAFT_FEE, -1, "Success", "Overdraft crossing fee applied");
        }
        return true;
    }
    
    public static double getMinimumBalance() {
        return MINIMUM_BALANCE;
    }
    public static double getOverdraftLimit() {
        return OVERDRAFT_LIMIT;
    }
    
    public static double getOverdraftFee() {
        return OVERDRAFT_FEE;
    }
    
    public static double getDailyLimit() {
        return DAILY_LIMIT;
    }
}