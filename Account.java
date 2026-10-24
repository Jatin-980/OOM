package Banking_system.Code;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;


public abstract class Account {
    private int accountNo ;
    private int customerId; 
    private double balance;
    private AccountStatus  status;
    private LocalDate opened;

    // An account "Has-A" list of transactions
    private List<Transaction> transactionHistory = new ArrayList<>();

    
    public void setTransactionHistory(List<Transaction> transactionHistory) {
        this.transactionHistory = transactionHistory;
    }
    
    public Account(int accountNo, int customerId, double balance, AccountStatus status,
        LocalDate opened) {
            this.accountNo = accountNo;
            this.customerId = customerId;
            this.balance = balance;
            this.status = status;
            this.opened = opened;
        }

    protected void recordTransaction(String type, double amount, int destinationAccount, String status, String reason) {
        String txId = "TXN" + System.currentTimeMillis() + "_" + (this.transactionHistory.size() + 1);

        Transaction tx = new Transaction(
            txId,
            type,
            amount,
            this.accountNo,      // Source account
            destinationAccount,  // Destination account (-1 if not a transfer)
            status,
            reason,
            this.getBalance()
        );
        this.transactionHistory.add(tx);
    }

    // Helper method to calculate today's total outgoing money (FR-11)
    protected double getTodayOutwardTotal() {
        double total = 0.0;
        LocalDate today = LocalDate.now();
        
        for (Transaction t : transactionHistory) {
            // Check if the transaction happened today, was successful, and moved money out
            if (t.getTimestamp().toLocalDate().equals(today) && 
                t.getStatus().equals("Success") && 
                (t.getType().equals("Withdrawal") || t.getType().equals("Transfer-out"))) {
                
                total += t.getAmount();
            }
        }
        return total;
    }

        
    // Abstract behaviors that child classes MUST implement
    public abstract AccountType getAccountType();
    public abstract boolean deposit(double amount);
    public abstract boolean withdraw(double amount);
    public abstract boolean transferOut(double amount, int destAccountNo);

    public boolean transferIn(double amount, int sourceAccountNo) {
        if (amount <= 0) {
            return false;
        }
        setBalance(getBalance() + amount);
        // Note: For Transfer-in, we store sourceAccountNo in the destinationAccount field or pass it cleanly
        String txId = "TXN" + System.currentTimeMillis() + "_" + (this.transactionHistory.size() + 1);
        Transaction tx = new Transaction(
            txId, "Transfer-in", amount, sourceAccountNo, this.accountNo, "Success", "None", this.getBalance()
        );
        this.transactionHistory.add(tx);
        return true;
    }
        
    public List<Transaction> getTransactionHistory() {
        return transactionHistory;
    }
    public int getAccountNo() {
        return accountNo;
    }
        
    public void setAccountNo(int accountNo) {
        this.accountNo = accountNo;
    }

    public int getCustomerId() {
        return customerId;
    }

    public void setCustomerId(int customerId) {
        this.customerId = customerId;
    }


    public double getBalance() {
        return balance;
    }

    public void setBalance(double balance) {
        this.balance = balance;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public LocalDate getOpened() {
        return opened;
    }

    public void setOpened(LocalDate opened) {
        this.opened = opened;
    }

    public enum AccountType {
        SAVINGS,
        CURRENT,
        FIXED_DEPOSIT
    }
    
    public enum AccountStatus {
        ACTIVE,
        FROZEN,
        CLOSED
    }
}
