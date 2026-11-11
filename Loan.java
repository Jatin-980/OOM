package Banking_system.Code;

import java.time.LocalDate;
import java.time.YearMonth;

public class Loan extends Account implements MonthlyInterestBearing {

    private double principalAmount;
    private double interestRate; // Annual interest rate percentage (e.g., 8.5 for 8.5% p.a.)
    private int loanDurationInMonths;

    public Loan(int accountNo, int customerId, double balance, AccountStatus status, LocalDate opened,
                double principalAmount, double interestRate, int loanDurationInMonths) {
        super(accountNo, customerId, balance, status, opened);
        this.principalAmount = principalAmount;
        this.interestRate = interestRate;
        this.loanDurationInMonths = loanDurationInMonths;
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.LOAN;
    }

    // Customer repays a portion or all of the outstanding loan balance
    @Override
    public boolean deposit(double amount) {
        if (amount <= 0) {
            recordTransaction("Deposit", amount, -1, "Failed", "Amount must be positive");
            return false;
        }

        if (amount > getBalance()) {
            recordTransaction("Deposit", amount, -1, "Failed", "Payment amount exceeds outstanding loan balance");
            return false;
        }

        double newBalance = Math.round((getBalance() - amount) * 100.0) / 100.0;
        setBalance(newBalance);
        recordTransaction("Deposit", amount, -1, "Success", "Loan installment repayment");

        if (newBalance == 0.0) {
            setStatus(AccountStatus.CLOSED);
        }
        return true;
    }

    // Direct withdrawals are not permitted on a disbursed loan
    @Override
    public boolean withdraw(double amount) {
        recordTransaction("Withdrawal", amount, -1, "Failed", "Withdrawals are not permitted from Loan accounts");
        return false;
    }

    // Transfers out are not permitted from a loan account
    @Override
    public boolean transferOut(double amount, int destAccountNo) {
        recordTransaction("Transfer-out", amount, destAccountNo, "Failed", "Transfers out are not permitted from Loan accounts");
        return false;
    }

    // Repay loan via transfer from another account (e.g. Savings or Current)
    @Override
    public boolean transferIn(double amount, int sourceAccountNo) {
        if (amount <= 0) {
            return false;
        }

        if (amount > getBalance()) {
            recordTransaction("Transfer-in", amount, sourceAccountNo, "Failed", "Transfer amount exceeds outstanding loan balance");
            return false;
        }

        double newBalance = Math.round((getBalance() - amount) * 100.0) / 100.0;
        setBalance(newBalance);

        String txId = "TXN" + System.currentTimeMillis() + "_" + (getTransactionHistory().size() + 1);
        Transaction tx = new Transaction(
            txId, "Transfer-in", amount, sourceAccountNo, this.getAccountNo(), "Success", "Loan repayment via transfer", this.getBalance()
        );
        getTransactionHistory().add(tx);

        if (newBalance == 0.0) {
            setStatus(AccountStatus.CLOSED);
        }
        return true;
    }

    // Check if monthly loan interest was already debited during the current calendar month
    public boolean hasReceivedInterestThisMonth() {
        YearMonth currentMonth = YearMonth.now();
        for (Transaction tx : getTransactionHistory()) {
            if ("Success".equals(tx.getStatus()) &&
                YearMonth.from(tx.getTimestamp()).equals(currentMonth) &&
                tx.getFailureReason() != null &&
                tx.getFailureReason().contains("Monthly loan interest debited")) {
                return true;
            }
        }
        return false;
    }

    // Calculates and debits monthly interest based on the interest rate and current loan balance
    public double debitMonthlyInterest() {
        if (getBalance() <= 0) {
            return 0.0;
        }

        // Monthly interest = balance * (annualRate / 100) / 12
        double rawInterest = getBalance() * (this.interestRate / 100.0) / 12.0;
        double interest = Math.round(rawInterest * 100.0) / 100.0;

        if (interest > 0) {
            double newBalance = Math.round((getBalance() + interest) * 100.0) / 100.0;
            setBalance(newBalance);
            recordTransaction("Fee", interest, -1, "Success", "Monthly loan interest debited");
        }

        return interest;
    }

    @Override
    public String processMonthlyInterestCycle() {
        double prevBalance = getBalance();
        double interest = debitMonthlyInterest();
        if (interest > 0) {
            return String.format(
                "Loan Account %d: Previous Outstanding = Rs. %.2f | Interest Debited = Rs. %.2f | New Balance = Rs. %.2f",
                getAccountNo(), prevBalance, interest, getBalance()
            );
        }
        return "";
    }

    @Override
    public String getSpecificDetails() {
        return String.format("Principal: Rs. %.2f, Interest Rate: %.1f%% p.a., Duration: %d months",
                principalAmount, interestRate, loanDurationInMonths);
    }

    // Getters and setters for Loan-specific instance variables
    public double getPrincipalAmount() {
        return principalAmount;
    }

    public void setPrincipalAmount(double principalAmount) {
        this.principalAmount = principalAmount;
    }

    public double getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(double interestRate) {
        this.interestRate = interestRate;
    }

    public int getLoanDurationInMonths() {
        return loanDurationInMonths;
    }

    public void setLoanDurationInMonths(int loanDurationInMonths) {
        this.loanDurationInMonths = loanDurationInMonths;
    }
}

