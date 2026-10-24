package Banking_system.Code;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Scanner;

public class CustomerMenu {
    
    private static Customer authenticateCustomer(Scanner sc, Bank bank) {
        System.out.print("Enter Customer ID: ");
        int customerId;

        try {
            customerId = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid Customer ID format.");
            return null;
        }

        if (!bank.findCustomer(customerId)) {
            System.out.println("Customer not found.");
            return null;
        }

        Customer customer = bank.getCustomer(customerId);

        // FR-2: Check if login is already locked
        if (customer.isLocked()) {
            System.out.println("Your account login is locked due to multiple failed attempts. Please contact a Bank Operator.");
            return null;
        }

        // Give up to 3 attempts in this session (or remaining attempts before hitting 3)
        while (customer.getFailedLoginAttempts() < 3) {
            System.out.print("Enter PIN: ");
            String inputPin = sc.nextLine().trim();

            if (customer.getPin().equals(inputPin)) {
                customer.setFailedLoginAttempts(0);
                BankStorage.saveData(bank);
                System.out.println("Login successful. Welcome, " + customer.getName() + ".");
                return customer;
            } else {
                int attempts = customer.getFailedLoginAttempts() + 1;
                customer.setFailedLoginAttempts(attempts);

                if (attempts >= 3) {
                    customer.setLocked(true);
                    BankStorage.saveData(bank);
                    System.out.println("Incorrect PIN. 3 failed attempts reached! Your login is now LOCKED.");
                    return null;
                } else {
                    BankStorage.saveData(bank);
                    System.out.println("Incorrect PIN. Attempts remaining: " + (3 - attempts));
                }
            }
        }

        return null;
    }

    // Reusable helper: Shows owned accounts and returns the one selected by Account Number
    private static Account selectCustomerAccount(Scanner sc, Bank bank, Customer customer) {
        if (customer.getOwnedAccountList().isEmpty()) {
            System.out.println("You do not have any accounts yet.");
            return null;
        }

        System.out.println("\n--- Your Accounts ---");
        for (int accNo : customer.getOwnedAccountList()) {
            Account acc = bank.getAccountMap().get(accNo);
            if (acc != null) {
                System.out.printf("   -> Account No: %d | Type: %s | Status: %s%n",
                    acc.getAccountNo(), acc.getAccountType(), acc.getStatus());
            }
        }

        while (true) {
            System.out.print("Enter Account Number (or 0 to cancel): ");
            try {
                int selectedAccNo = Integer.parseInt(sc.nextLine().trim());

                if (selectedAccNo == 0) {
                    return null;
                }

                // Ensure the customer actually owns this account number
                if (!customer.getOwnedAccountList().contains(selectedAccNo)) {
                    System.out.println("Invalid choice! You do not own an account with number " + selectedAccNo + ".");
                    continue;
                }

                Account selectedAccount = bank.getAccountMap().get(selectedAccNo);
                if (selectedAccount != null) {
                    return selectedAccount;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input! Please enter a valid numeric Account Number.");
            }
        }
    }

    // Option 1: View Balance
    private static void view_balance(Scanner sc, Bank bank, Customer customer) {
        Account selectedAcc = selectCustomerAccount(sc, bank, customer);
        if (selectedAcc == null) {
            return; // User had no accounts or pressed 0 to cancel
        }

        System.out.println("\n========== ACCOUNT BALANCE ==========");
        System.out.printf("Account Number : %d%n", selectedAcc.getAccountNo());
        System.out.printf("Account Type   : %s%n", selectedAcc.getAccountType());
        System.out.printf("Account Status : %s%n", selectedAcc.getStatus());
        System.out.printf("Current Balance: Rs. %.2f%n", selectedAcc.getBalance());
        System.out.println("=====================================\n");
    }

    // Helper to get the most recently recorded transaction on an account
    private static Transaction getLastTransaction(Account account) {
        java.util.List<Transaction> history = account.getTransactionHistory();
        if (history == null || history.isEmpty()) {
            return null;
        }
        return history.get(history.size() - 1);
    }

    // Option 2: Deposit
    private static void deposit(Scanner sc, Bank bank, Customer customer) {
        Account selectedAccount = selectCustomerAccount(sc, bank, customer);
        if (selectedAccount == null) {
            return;
        }

        System.out.print("Enter deposit amount: Rs. ");
        double amount;
        try {
            amount = Double.parseDouble(sc.nextLine().trim());
            if (!Double.isFinite(amount)) {
                System.out.println("Invalid amount entered.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input! Please enter a valid numeric amount.");
            return;
        }

        // Reject and log if the account is Frozen or Closed (FR-21)
        if (selectedAccount.getStatus() != Account.AccountStatus.ACTIVE) {
            selectedAccount.recordTransaction(
                "Deposit", amount, -1, "Failed", "Account is not Active (" + selectedAccount.getStatus() + ")"
            );
            BankStorage.saveData(bank);
            System.out.println("Deposit failed! Reason: Account is " + selectedAccount.getStatus() + ".");
            return;
        }

        boolean success = selectedAccount.deposit(amount);
        BankStorage.saveData(bank); // Persist balance update and/or transaction log (FR-20, FR-21)

        Transaction lastTx = getLastTransaction(selectedAccount);

        if (success) {
            System.out.println("Deposit successful. Transaction ID: " + (lastTx != null ? lastTx.getTransactionId() : "N/A"));
            System.out.printf("New balance for Account %d: Rs. %.2f%n", selectedAccount.getAccountNo(), selectedAccount.getBalance());
        } else {
            String reason = (lastTx != null) ? lastTx.getFailureReason() : "Invalid deposit";
            System.out.println("Deposit failed! Reason: " + reason);
        }
    }

    // Option 3: Withdraw (follows the exact same structure!)
    private static void withdraw(Scanner sc, Bank bank, Customer customer) {
        Account selectedAccount = selectCustomerAccount(sc, bank, customer);
        if (selectedAccount == null) {
            return;
        }

        System.out.print("Enter withdrawal amount: Rs. ");
        double amount;
        try {
            amount = Double.parseDouble(sc.nextLine().trim());
            if (!Double.isFinite(amount)) {
                System.out.println("Invalid amount entered.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input! Please enter a valid numeric amount.");
            return;
        }

        // Reject and log if the account is Frozen or Closed (FR-21)
        if (selectedAccount.getStatus() != Account.AccountStatus.ACTIVE) {
            selectedAccount.recordTransaction(
                "Withdrawal", amount, -1, "Failed", "Account is not Active (" + selectedAccount.getStatus() + ")"
            );
            BankStorage.saveData(bank);
            System.out.println("Withdrawal failed! Reason: Account is " + selectedAccount.getStatus() + ".");
            return;
        }

        boolean success = selectedAccount.withdraw(amount);
        BankStorage.saveData(bank);

        Transaction lastTx = getLastTransaction(selectedAccount);

        if (success) {
            System.out.println("Withdrawal successful. Transaction ID: " + (lastTx != null ? lastTx.getTransactionId() : "N/A"));
            System.out.printf("New balance for Account %d: Rs. %.2f%n", selectedAccount.getAccountNo(), selectedAccount.getBalance());
        } else {
            String reason = (lastTx != null) ? lastTx.getFailureReason() : "Invalid withdrawal";
            System.out.println("Withdrawal failed! Reason: " + reason);
        }
    }

    // Option 4: Transfer Funds (FR-12, FR-13, FR-21)
    private static void transfer_funds(Scanner sc, Bank bank, Customer customer) {
        // 1. Select source account
        Account sourceAcc = selectCustomerAccount(sc, bank, customer);
        if (sourceAcc == null) {
            return;
        }

        // 2. Ask for destination account number
        System.out.print("Enter Destination Account Number: ");
        int destAccNo;
        try {
            destAccNo = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input! Please enter a numeric Account Number.");
            return;
        }

        // 3. Ask for transfer amount
        System.out.print("Enter transfer amount: Rs. ");
        double amount;
        try {
            amount = Double.parseDouble(sc.nextLine().trim());
            if (!Double.isFinite(amount)) {
                System.out.println("Invalid amount entered.");
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input! Please enter a valid numeric amount.");
            return;
        }

        // 4. All-or-Nothing Pre-Validation (FR-12, FR-13, FR-21)
        if (sourceAcc.getStatus() != Account.AccountStatus.ACTIVE) {
            sourceAcc.recordTransaction("Transfer-out", amount, destAccNo, "Failed", "Source account is not Active");
            BankStorage.saveData(bank);
            System.out.println("Transfer failed! Reason: Your source account is " + sourceAcc.getStatus() + ".");
            return;
        }

        if (sourceAcc.getAccountNo() == destAccNo) {
            sourceAcc.recordTransaction("Transfer-out", amount, destAccNo, "Failed", "Source and destination accounts cannot be the same");
            BankStorage.saveData(bank);
            System.out.println("Transfer failed! Reason: Cannot transfer to the same account.");
            return;
        }

        Account destAcc = bank.getAccountMap().get(destAccNo);
        if (destAcc == null) {
            sourceAcc.recordTransaction("Transfer-out", amount, destAccNo, "Failed", "Destination account does not exist");
            BankStorage.saveData(bank);
            System.out.println("Transfer failed! Reason: Destination account " + destAccNo + " does not exist.");
            return;
        }

        if (destAcc.getStatus() != Account.AccountStatus.ACTIVE) {
            sourceAcc.recordTransaction("Transfer-out", amount, destAccNo, "Failed", "Destination account is not Active");
            BankStorage.saveData(bank);
            System.out.println("Transfer failed! Reason: Destination account is " + destAcc.getStatus() + ".");
            return;
        }

        if (destAcc.getAccountType() == Account.AccountType.FIXED_DEPOSIT) {
            sourceAcc.recordTransaction("Transfer-out", amount, destAccNo, "Failed", "Cannot transfer into a Fixed Deposit account");
            BankStorage.saveData(bank);
            System.out.println("Transfer failed! Reason: Fixed Deposit accounts do not accept incoming transfers.");
            return;
        }

        // 5. Execute the transfer (Debit source -> Credit destination)
        if (sourceAcc.transferOut(amount, destAccNo)) {
            destAcc.transferIn(amount, sourceAcc.getAccountNo());
            BankStorage.saveData(bank);

            Transaction lastTx = getLastTransaction(sourceAcc);
            System.out.println("Transfer successful! Transaction ID: " + (lastTx != null ? lastTx.getTransactionId() : "N/A"));
            System.out.printf("Transferred Rs. %.2f from Account %d to Account %d.%n", amount, sourceAcc.getAccountNo(), destAccNo);
            System.out.printf("New balance for Account %d: Rs. %.2f%n", sourceAcc.getAccountNo(), sourceAcc.getBalance());
        } else {
            BankStorage.saveData(bank);
            Transaction lastTx = getLastTransaction(sourceAcc);
            String reason = (lastTx != null) ? lastTx.getFailureReason() : "Transfer rejected";
            System.out.println("Transfer failed! Reason: " + reason);
        }
    }

    // Option 5: View Recent Transactions (FR-17)
    private static void view_recent_transactions(Scanner sc, Bank bank, Customer customer) {
        Account selectedAcc = selectCustomerAccount(sc, bank, customer);
        if (selectedAcc == null) {
            return;
        }

        int count = 10; // Default to last 10 (FR-17)
        System.out.print("Enter number of recent transactions to view (Press Enter for default 10): ");
        String input = sc.nextLine().trim();

        if (!input.isEmpty()) {
            try {
                int parsed = Integer.parseInt(input);
                if (parsed <= 0) {
                    System.out.println("Count must be positive. Using default of 10.");
                } else {
                    count = parsed;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid number entered. Using default of 10.");
            }
        }

        java.util.List<Transaction> history = selectedAcc.getTransactionHistory();
        System.out.println("\n================ RECENT TRANSACTIONS ================");
        System.out.printf("Account No: %d | Type: %s | Current Balance: Rs. %.2f%n",
                selectedAcc.getAccountNo(), selectedAcc.getAccountType(), selectedAcc.getBalance());
        System.out.println("-----------------------------------------------------");

        if (history.isEmpty()) {
            System.out.println("No transactions found for this account.");
            System.out.println("=====================================================\n");
            return;
        }

        int startIndex = Math.max(0, history.size() - count);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        for (int i = startIndex; i < history.size(); i++) {
            Transaction tx = history.get(i);
            System.out.printf("[%s] ID: %s | Type: %-12s | Amt: Rs. %8.2f | Status: %-7s | Bal: Rs. %9.2f | Note: %s%n",
                    tx.getTimestamp().format(dtf),
                    tx.getTransactionId(),
                    tx.getType(),
                    tx.getAmount(),
                    tx.getStatus(),
                    tx.getResultingBalance(),
                    tx.getFailureReason());
        }
        System.out.println("=====================================================\n");
    }

    // Option 6: Generate Statement for Date Range & Save to File (FR-18, FR-19)
    private static void generate_statement(Scanner sc, Bank bank, Customer customer) {
        Account selectedAcc = selectCustomerAccount(sc, bank, customer);
        if (selectedAcc == null) {
            return;
        }

        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd-MM-uuuu")
                .withResolverStyle(java.time.format.ResolverStyle.STRICT);
        LocalDate startDate;
        LocalDate endDate;

        while (true) {
            try {
                System.out.print("Enter Start Date (dd-MM-yyyy): ");
                startDate = LocalDate.parse(sc.nextLine().trim(), dateFormatter);

                System.out.print("Enter End Date (dd-MM-yyyy): ");
                endDate = LocalDate.parse(sc.nextLine().trim(), dateFormatter);

                if (endDate.isBefore(startDate)) {
                    System.out.println("End date cannot be earlier than start date! Try again.");
                    continue;
                }
                break;
            } catch (java.time.format.DateTimeParseException e) {
                System.out.println("Invalid date format! Please use dd-MM-yyyy.");
            }
        }

        java.util.List<Transaction> history = selectedAcc.getTransactionHistory();
        java.util.List<Transaction> txInRange = new java.util.ArrayList<>();

        double openingBalance = 0.0;
        boolean openingSet = false;
        double totalCredits = 0.0;
        double totalDebits = 0.0;

        for (Transaction tx : history) {
            LocalDate txDate = tx.getTimestamp().toLocalDate();
            boolean isSuccess = "Success".equals(tx.getStatus());
            boolean isCredit = "Deposit".equals(tx.getType()) ||
                               "Transfer-in".equals(tx.getType()) ||
                               "Interest".equals(tx.getType());

            // Track balance prior to startDate
            if (txDate.isBefore(startDate)) {
                if (isSuccess) {
                    openingBalance = tx.getResultingBalance();
                    openingSet = true;
                }
            } else if (!txDate.isAfter(endDate)) {
                // First successful transaction on or after startDate helps establish openingBalance if none prior
                if (!openingSet && isSuccess) {
                    openingBalance = isCredit
                            ? (tx.getResultingBalance() - tx.getAmount())
                            : (tx.getResultingBalance() + tx.getAmount());
                    openingSet = true;
                }

                txInRange.add(tx);

                if (isSuccess) {
                    if (isCredit) {
                        totalCredits += tx.getAmount();
                    } else {
                        totalDebits += tx.getAmount();
                    }
                }
            } else {
                // Transaction is after endDate
                if (!openingSet && isSuccess) {
                    openingBalance = isCredit
                            ? (tx.getResultingBalance() - tx.getAmount())
                            : (tx.getResultingBalance() + tx.getAmount());
                    openingSet = true;
                }
            }
        }

        // If no transactions ever occurred, or account opened before startDate with no tx
        if (!openingSet) {
            if (!startDate.isBefore(selectedAcc.getOpened())) {
                openingBalance = selectedAcc.getBalance();
            } else {
                openingBalance = 0.0;
            }
        }

        double closingBalance = openingBalance + totalCredits - totalDebits;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        // Build the statement text using StringBuilder so we can both print and save it (FR-19)
        StringBuilder sb = new StringBuilder();
        sb.append("\n==================== ACCOUNT STATEMENT ====================\n");
        sb.append(String.format("Customer Name   : %s (ID: %d)%n", customer.getName(), customer.getCustomerID()));
        sb.append(String.format("Account Number  : %d (%s)%n", selectedAcc.getAccountNo(), selectedAcc.getAccountType()));
        sb.append(String.format("Statement Period: %s to %s%n", startDate, endDate));
        sb.append("-----------------------------------------------------------\n");
        sb.append(String.format("Opening Balance : Rs. %.2f%n", openingBalance));
        sb.append("-----------------------------------------------------------\n");

        if (txInRange.isEmpty()) {
            sb.append("No transactions recorded during this period.\n");
        } else {
            for (Transaction tx : txInRange) {
                sb.append(String.format("[%s] %-12s | %-12s | Rs. %8.2f | %-7s | Bal: Rs. %9.2f | %s%n",
                        tx.getTimestamp().format(dtf),
                        tx.getTransactionId(),
                        tx.getType(),
                        tx.getAmount(),
                        tx.getStatus(),
                        tx.getResultingBalance(),
                        tx.getFailureReason()));
            }
        }

        sb.append("-----------------------------------------------------------\n");
        sb.append(String.format("Total Credits   : Rs. %.2f%n", totalCredits));
        sb.append(String.format("Total Debits    : Rs. %.2f%n", totalDebits));
        sb.append(String.format("Closing Balance : Rs. %.2f%n", closingBalance));
        sb.append("===========================================================\n");

        // 1. Print on screen (FR-18)
        System.out.print(sb.toString());

        // 2. Offer to save to a text file (FR-19)
        System.out.print("Would you like to save this statement to a text file? (y/n): ");
        String saveChoice = sc.nextLine().trim();
        if (saveChoice.equalsIgnoreCase("y")) {
            String fileName = "Statement_Acc" + selectedAcc.getAccountNo() + "_" + startDate + "_to_" + endDate + ".txt";
            java.nio.file.Path filePath = java.nio.file.Paths.get("data", fileName);
            try {
                java.nio.file.Files.createDirectories(filePath.getParent());
                java.nio.file.Files.writeString(filePath, sb.toString());
                System.out.println("Statement saved successfully to: " + filePath.toAbsolutePath());
            } catch (java.io.IOException e) {
                System.out.println("Failed to save statement file: " + e.getMessage());
            }
        }
    }


    // Option 7: Open Additional Account OR Close Account / FD (FR-7, FR-15, FR-16)
    private static void manage_accounts(Scanner sc, Bank bank, Customer customer) {
        System.out.println("\n1. Open a New Additional Account");
        System.out.println("2. Close an Existing Account / Redeem Fixed Deposit");
        System.out.println("3. Cancel");
        System.out.print("Enter choice: ");

        int subChoice;
        try {
            subChoice = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid choice.");
            return;
        }

        if (subChoice == 1) {
            OperatorMenu.openAccountForKnownCustomer(sc, bank, customer);
        } else if (subChoice == 2) {
            close_account(sc, bank, customer);
        }
    }

    // Handles FR-7 (Closing Savings/Current at 0 balance) and FR-15/FR-16 (FD Maturity & Premature Closure)
    private static void close_account(Scanner sc, Bank bank, Customer customer) {
        Account selectedAcc = selectCustomerAccount(sc, bank, customer);
        if (selectedAcc == null) {
            return;
        }

        if (selectedAcc.getStatus() == Account.AccountStatus.CLOSED) {
            System.out.println("This account is already CLOSED.");
            return;
        }

        // Case A: Fixed Deposit Closure (FR-15 & FR-16)
        if (selectedAcc.getAccountType() == Account.AccountType.FIXED_DEPOSIT) {
            FixedDepositAccount fd = (FixedDepositAccount) selectedAcc;
            Account payoutAcc = bank.getAccountMap().get(fd.getLinkedSavingsAccountNo());

            if (payoutAcc == null || payoutAcc.getStatus() != Account.AccountStatus.ACTIVE) {
                System.out.println("Cannot close FD: Linked payout Savings Account (" + fd.getLinkedSavingsAccountNo() + ") is not Active.");
                return;
            }

            LocalDate maturityDate = fd.getOpened().plusMonths(fd.getTenureMonths());
            LocalDate today = LocalDate.now();
            double principal = fd.getBalance();
            double appliedRate;
            double interest;
            String closureType;

            if (!today.isBefore(maturityDate)) {
                appliedRate = fd.getInterestRate();
                interest = principal * (appliedRate / 100.0) * (fd.getTenureMonths() / 12.0);
                closureType = "Maturity Closure";
            } else {
                appliedRate = fd.getInterestRate() - 1.0;
                long monthsHeld = ChronoUnit.MONTHS.between(fd.getOpened(), today);
                long effectiveMonths = Math.max(1, monthsHeld);
                interest = principal * (appliedRate / 100.0) * (effectiveMonths / 12.0);
                closureType = "Premature Closure (1% penalty applied)";
            }

            interest = Math.round(interest * 100.0) / 100.0;
            double totalPayout = Math.round((principal + interest) * 100.0) / 100.0;

            fd.setBalance(totalPayout);
            fd.recordTransaction("Interest", interest, -1, "Success", closureType + " @ " + appliedRate + "%");

            fd.setBalance(0.0);
            fd.setStatus(Account.AccountStatus.CLOSED);
            fd.recordTransaction("Transfer-out", totalPayout, payoutAcc.getAccountNo(), "Success", "FD Closure Payout");

            payoutAcc.transferIn(totalPayout, fd.getAccountNo());
            BankStorage.saveData(bank);

            System.out.println("\n--- Fixed Deposit Closed (" + closureType + ") ---");
            System.out.printf("Principal        : Rs. %.2f%n", principal);
            System.out.printf("Interest Applied : %.2f%% (Rs. %.2f)%n", appliedRate, interest);
            System.out.printf("Total Credited   : Rs. %.2f -> Savings Account AC%d%n", totalPayout, payoutAcc.getAccountNo());
            System.out.printf("New Balance AC%d : Rs. %.2f%n", payoutAcc.getAccountNo(), payoutAcc.getBalance());
            return;
        }

        // Case B: Savings or Current Account Closure (FR-7: Balance must be 0)
        if (Math.abs(selectedAcc.getBalance()) > 0.001) {
            System.out.printf("Cannot close Account %d! Balance must be Rs. 0.00 (Current balance: Rs. %.2f).%n",
                    selectedAcc.getAccountNo(), selectedAcc.getBalance());
            System.out.println("Please withdraw or transfer out the remaining funds first.");
            return;
        }

        selectedAcc.setStatus(Account.AccountStatus.CLOSED);
        BankStorage.saveData(bank);
        System.out.println("Account " + selectedAcc.getAccountNo() + " has been successfully CLOSED.");
    }

    public static void menu(Scanner sc, Bank bank) {
        // Step 1: Authenticate before showing the menu (FR-1)
        Customer loggedInCustomer = authenticateCustomer(sc, bank);
        if (loggedInCustomer == null) {
            return; // Go straight back to Main Menu if login failed
        }

        int choice;
        while (true) {
            System.out.println("\n1. View Balance");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Transfer Funds");
            System.out.println("5. View Recent Transactions");
            System.out.println("6. Generate Statement");
            System.out.println("7. Open an Additional Account / Close Account");
            System.out.println("8. Logout");
            System.out.print("Enter your choice : ");

            if (sc.hasNextInt()) {
                choice = sc.nextInt();
                sc.nextLine(); // <-- FIXED: Consume newline!
                switch (choice) {
                    case 1:
                        view_balance(sc, bank, loggedInCustomer);
                        break;
                    case 2:
                        deposit(sc, bank, loggedInCustomer);
                        break;
                    case 3:
                        withdraw(sc, bank, loggedInCustomer);
                        break;
                    case 4:
                        transfer_funds(sc, bank, loggedInCustomer);
                        break;
                    case 5:
                        view_recent_transactions(sc, bank, loggedInCustomer);
                        break;
                    case 6:
                        generate_statement(sc, bank, loggedInCustomer);
                        break;
                    case 7:
                        manage_accounts(sc, bank, loggedInCustomer); // <-- FIXED: Hooked up Option 7!
                        break;
                    case 8:
                        System.out.println("Logged out. Goodbye.");
                        return; // <-- FIXED: Changed break to return!
                    default:
                        System.out.println("You Entered wrong choice , try again !!");
                }
            } else {
                System.out.println("Invalid input , try again !!");
                sc.nextLine();
            }
        }
    }

}
