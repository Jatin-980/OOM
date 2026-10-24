package Banking_system.Code;

import java.time.LocalDateTime;

public class Transaction {
    private String transactionId;
    private LocalDateTime timestamp;
    private String type; // Deposit, Withdrawal, Transfer-in, Transfer-out, etc.
    private double amount;
    private int sourceAccount;
    private int destinationAccount; // Optional, only for transfers
    private String status; // Success or Failed
    private String failureReason; 
    private double resultingBalance;
    
    // 1. Constructor for NEW transactions happening right now
    public Transaction(String transactionId, String type, double amount, int sourceAccount, int destinationAccount, String status, String failureReason, double resultingBalance) {
        this(transactionId, LocalDateTime.now(), type, amount, sourceAccount, destinationAccount, status, failureReason, resultingBalance);
    }

    // 2. Constructor for LOADING saved transactions from CSV (preserves original timestamp)
    public Transaction(String transactionId, LocalDateTime timestamp, String type, double amount, int sourceAccount, int destinationAccount, String status, String failureReason, double resultingBalance) {
        this.transactionId = transactionId;
        this.timestamp = timestamp;
        this.type = type;
        this.amount = amount;
        this.sourceAccount = sourceAccount;
        this.destinationAccount = destinationAccount;
        this.status = status;
        this.failureReason = failureReason;
        this.resultingBalance = resultingBalance;
    }
    
    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public int getSourceAccount() {
        return sourceAccount;
    }

    public void setSourceAccount(int sourceAccount) {
        this.sourceAccount = sourceAccount;
    }

    public int getDestinationAccount() {
        return destinationAccount;
    }

    public void setDestinationAccount(int destinationAccount) {
        this.destinationAccount = destinationAccount;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public double getResultingBalance() {
        return resultingBalance;
    }

    public void setResultingBalance(double resultingBalance) {
        this.resultingBalance = resultingBalance;
    }

}