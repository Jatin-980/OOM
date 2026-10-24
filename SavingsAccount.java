package Banking_system.Code;

import java.time.LocalDate;
import java.time.YearMonth;

public class SavingsAccount extends Account {

    private static final double MINIMUM_BALANCE = 1000.0;
    private static final double ANNUAL_INTEREST_RATE = 4.0; // 4% per annum
    private static final double DAILY_LIMIT = 50000.0;

    public SavingsAccount(int accountNo, int customerId, double balance, AccountStatus status, LocalDate opened) {
        super(accountNo, customerId, balance, status, opened);
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.SAVINGS;
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

        // FR-9: Balance may never drop below Rs. 1,000
        if (getBalance() - amount < MINIMUM_BALANCE) {
            recordTransaction("Withdrawal", amount, -1, "Failed", "Cannot drop below minimum balance of Rs. 1,000");
            return false;
        }

        // FR-11: Combined daily outward limit of Rs. 50,000
        if (getTodayOutwardTotal() + amount > DAILY_LIMIT) {
            recordTransaction("Withdrawal", amount, -1, "Failed", "Daily withdrawal limit of Rs. 50,000 exceeded");
            return false;
        }

        setBalance(getBalance() - amount);
        recordTransaction("Withdrawal", amount, -1, "Success", "None");
        return true;
    }

    // Checks if interest was already credited during the current calendar month
    public boolean hasReceivedInterestThisMonth() {
        YearMonth currentMonth = YearMonth.now();
        for (Transaction tx : getTransactionHistory()) {
            if ("Interest".equals(tx.getType()) &&
                "Success".equals(tx.getStatus()) &&
                YearMonth.from(tx.getTimestamp()).equals(currentMonth)) {
                return true;
            }
        }
        return false;
    }

    // Finds the lowest balance held by this account during the current month
    public double getLowestBalanceThisMonth() {
        YearMonth currentMonth = YearMonth.now();
        double lowestBalance = getBalance();
        boolean foundTxThisMonth = false;

        for (Transaction tx : getTransactionHistory()) {
            if (!"Success".equals(tx.getStatus())) {
                continue;
            }

            if (YearMonth.from(tx.getTimestamp()).equals(currentMonth)) {
                // Capture the balance right before the first transaction of the month occurred
                if (!foundTxThisMonth) {
                    foundTxThisMonth = true;
                    double balanceBeforeFirstTx = getBalanceBeforeTransaction(tx);
                    if (balanceBeforeFirstTx < lowestBalance) {
                        lowestBalance = balanceBeforeFirstTx;
                    }
                }

                // Compare against the balance after each transaction this month
                if (tx.getResultingBalance() < lowestBalance) {
                    lowestBalance = tx.getResultingBalance();
                }
            }
        }

        return lowestBalance;
    }

    private double getBalanceBeforeTransaction(Transaction tx) {
        String type = tx.getType();
        if ("Deposit".equals(type) || "Transfer-in".equals(type) || "Interest".equals(type)) {
            return tx.getResultingBalance() - tx.getAmount();
        } else {
            // Withdrawal, Transfer-out, or Fee
            return tx.getResultingBalance() + tx.getAmount();
        }
    }

    // FR-14: Calculates and credits monthly interest, returning the credited amount
    public double creditMonthlyInterest() {
        double lowestBalance = getLowestBalanceThisMonth();
        if (lowestBalance <= 0) {
            return 0.0;
        }

        // 4% per annum = (4 / 100) / 12 per month
        double rawInterest = lowestBalance * (ANNUAL_INTEREST_RATE / 100.0) / 12.0;
        double interest = Math.round(rawInterest * 100.0) / 100.0;

        if (interest > 0) {
            setBalance(Math.round((getBalance() + interest) * 100.0) / 100.0);
            recordTransaction("Interest", interest, -1, "Success", "None");
        }

        return interest;
    }
    @Override
    public boolean transferOut(double amount, int destAccountNo) {
        if (amount <= 0) {
            recordTransaction("Transfer-out", amount, destAccountNo, "Failed", "Amount must be positive");
            return false;
        }
        if (getBalance() - amount < MINIMUM_BALANCE) {
            recordTransaction("Transfer-out", amount, destAccountNo, "Failed", "Cannot drop below minimum balance of Rs. 1,000");
            return false;
        }
        if (getTodayOutwardTotal() + amount > DAILY_LIMIT) {
            recordTransaction("Transfer-out", amount, destAccountNo, "Failed", "Daily outward limit of Rs. 50,000 exceeded");
            return false;
        }

        setBalance(getBalance() - amount);
        recordTransaction("Transfer-out", amount, destAccountNo, "Success", "None");
        return true;
    }

    public static double getMinimumBalance() {
        return MINIMUM_BALANCE;
    }

    public static double getAnnualInterestRate() {
        return ANNUAL_INTEREST_RATE;
    }

    public static double getDailyLimit() {
        return DAILY_LIMIT;
    }
}