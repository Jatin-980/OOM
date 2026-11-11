package Banking_system.Code;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class CustomerMenu {
    
    private static void validateCustomerAccess(Bank bank, int customerId) throws AuthenticationException {
        if (!bank.findCustomer(customerId)) {
            throw new AuthenticationException("Customer with ID " + customerId + " not found.");
        }
        Customer customer = bank.getCustomer(customerId);
        if (customer.isLocked()) {
            throw new AuthenticationException("Your account login is locked due to multiple failed attempts. Please contact a Bank Operator.");
        }
    }

    private static Customer authenticateCustomer(Scanner sc, Bank bank) {
        ConsoleUI.printPrompt("Enter Customer ID: ");
        int customerId;

        try {
            customerId = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            ConsoleUI.printError("Invalid Customer ID format.");
            return null;
        }

        Customer customer;
        try {
            validateCustomerAccess(bank, customerId);
            customer = bank.getCustomer(customerId);
        } catch (AuthenticationException e) {
            ConsoleUI.printError(e.getMessage());
            return null;
        }

        // Give up to 3 attempts in this session (or remaining attempts before hitting 3)
        while (customer.getFailedLoginAttempts() < 3) {
            ConsoleUI.printPrompt("Enter PIN: ");
            String inputPin = sc.nextLine().trim();

            if (customer.getPin().equals(inputPin)) {
                customer.setFailedLoginAttempts(0);
                BankStorage.saveData(bank);
                ConsoleUI.printSuccess("Login successful. Welcome, " + customer.getName() + ".");
                return customer;
            } else {
                int attempts = customer.getFailedLoginAttempts() + 1;
                customer.setFailedLoginAttempts(attempts);

                if (attempts >= 3) {
                    customer.setLocked(true);
                    BankStorage.saveData(bank);
                    try {
                        throw new AuthenticationException("Incorrect PIN. 3 failed attempts reached! Your login is now LOCKED.");
                    } catch (AuthenticationException e) {
                        ConsoleUI.printError(e.getMessage());
                    }
                    return null;
                } else {
                    BankStorage.saveData(bank);
                    ConsoleUI.printError("Incorrect PIN. Attempts remaining: " + (3 - attempts));
                }
            }
        }

        return null;
    }

    // Reusable helper: Shows owned accounts and returns the one selected by Account Number
    private static Account selectCustomerAccount(Scanner sc, Bank bank, Customer customer) {
        if (customer.getOwnedAccountList().isEmpty()) {
            ConsoleUI.printInfo("You do not have any accounts yet.");
            return null;
        }

        ConsoleUI.printSectionHeader("YOUR ACCOUNTS");
        String[] headers = {"Acc No", "Account Type", "Status", "Current Balance (Rs.)",
                "Account Specifics / Rules"};
        int[] widths = {8, 16, 12, 22, 34};
        List<String[]> rows = new ArrayList<>();
        for (int accNo : customer.getOwnedAccountList()) {
            Account acc = bank.getAccountMap().get(accNo);
            if (acc != null) {
                rows.add(accountTableRow(acc));
            }
        }
        ConsoleUI.printTable(headers, rows, widths, -1);

        while (true) {
            ConsoleUI.printPrompt("Enter Account Number (or 0 to cancel): ");
            try {
                int selectedAccNo = Integer.parseInt(sc.nextLine().trim());

                if (selectedAccNo == 0) {
                    return null;
                }

                // Ensure the customer actually owns this account number
                if (!customer.getOwnedAccountList().contains(selectedAccNo)) {
                    ConsoleUI.printError("Invalid choice! You do not own an account with number "
                            + selectedAccNo + ".");
                    continue;
                }

                Account selectedAccount = bank.getAccountMap().get(selectedAccNo);
                if (selectedAccount != null) {
                    return selectedAccount;
                }
            } catch (NumberFormatException e) {
                ConsoleUI.printError("Invalid input! Please enter a valid numeric Account Number.");
            }
        }
    }

    // Option 1: View Balance
    private static void view_balance(Scanner sc, Bank bank, Customer customer) {
        Account selectedAcc = selectCustomerAccount(sc, bank, customer);
        if (selectedAcc == null) {
            return; // User had no accounts or pressed 0 to cancel
        }

        ConsoleUI.printSectionHeader("ACCOUNT BALANCE");
        ConsoleUI.printTable(
                new String[]{"Acc No", "Account Type", "Status", "Current Balance (Rs.)",
                        "Account Specifics / Rules"},
                java.util.Collections.singletonList(accountTableRow(selectedAcc)),
                new int[]{8, 16, 12, 22, 34},
                -1);
    }

    private static String[] accountTableRow(Account account) {
        return new String[]{
                String.valueOf(account.getAccountNo()),
                String.valueOf(account.getAccountType()),
                String.valueOf(account.getStatus()),
                String.format("%.2f", account.getBalance()),
                account.getSpecificDetails()
        };
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

        ConsoleUI.printPrompt("Enter deposit amount: Rs. ");
        double amount;
        try {
            amount = Double.parseDouble(sc.nextLine().trim());
            if (!Double.isFinite(amount)) {
                ConsoleUI.printError("Invalid amount entered.");
                return;
            }
        } catch (NumberFormatException e) {
            ConsoleUI.printError("Invalid input! Please enter a valid numeric amount.");
            return;
        }

        // Reject and log if the account is Frozen or Closed (FR-21)
        if (selectedAccount.getStatus() != Account.AccountStatus.ACTIVE) {
            selectedAccount.recordTransaction(
                "Deposit", amount, -1, "Failed", "Account is not Active (" + selectedAccount.getStatus() + ")"
            );
            BankStorage.saveData(bank);
            ConsoleUI.printError("Deposit failed! Reason: Account is " + selectedAccount.getStatus() + ".");
            return;
        }

        boolean success = selectedAccount.deposit(amount);
        BankStorage.saveData(bank); // Persist balance update and/or transaction log (FR-20, FR-21)

        Transaction lastTx = getLastTransaction(selectedAccount);

        if (success) {
            ConsoleUI.printSuccess("Deposit successful. Transaction ID: "
                    + (lastTx != null ? lastTx.getTransactionId() : "N/A"));
            ConsoleUI.printInfo(String.format("New balance for Account %d: Rs. %.2f",
                    selectedAccount.getAccountNo(), selectedAccount.getBalance()));
        } else {
            String reason = (lastTx != null) ? lastTx.getFailureReason() : "Invalid deposit";
            ConsoleUI.printError("Deposit failed! Reason: " + reason);
        }
    }

    // Option 3: Withdraw (follows the exact same structure!)
    private static void withdraw(Scanner sc, Bank bank, Customer customer) {
        Account selectedAccount = selectCustomerAccount(sc, bank, customer);
        if (selectedAccount == null) {
            return;
        }

        ConsoleUI.printPrompt("Enter withdrawal amount: Rs. ");
        double amount;
        try {
            amount = Double.parseDouble(sc.nextLine().trim());
            if (!Double.isFinite(amount)) {
                ConsoleUI.printError("Invalid amount entered.");
                return;
            }
        } catch (NumberFormatException e) {
            ConsoleUI.printError("Invalid input! Please enter a valid numeric amount.");
            return;
        }

        // Reject and log if the account is Frozen or Closed (FR-21)
        if (selectedAccount.getStatus() != Account.AccountStatus.ACTIVE) {
            selectedAccount.recordTransaction(
                "Withdrawal", amount, -1, "Failed", "Account is not Active (" + selectedAccount.getStatus() + ")"
            );
            BankStorage.saveData(bank);
            ConsoleUI.printError("Withdrawal failed! Reason: Account is " + selectedAccount.getStatus() + ".");
            return;
        }

        boolean success = selectedAccount.withdraw(amount);
        BankStorage.saveData(bank);

        Transaction lastTx = getLastTransaction(selectedAccount);

        if (success) {
            ConsoleUI.printSuccess("Withdrawal successful. Transaction ID: "
                    + (lastTx != null ? lastTx.getTransactionId() : "N/A"));
            ConsoleUI.printInfo(String.format("New balance for Account %d: Rs. %.2f",
                    selectedAccount.getAccountNo(), selectedAccount.getBalance()));
        } else {
            String reason = (lastTx != null) ? lastTx.getFailureReason() : "Invalid withdrawal";
            ConsoleUI.printError("Withdrawal failed! Reason: " + reason);
        }
    }

    private static Account validateTransferPreConditions(Bank bank, Account sourceAcc, int destAccNo, double amount)
            throws InvalidTransactionException {
        if (sourceAcc.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new InvalidTransactionException("Your source account is " + sourceAcc.getStatus() + ".");
        }

        if (sourceAcc.getAccountNo() == destAccNo) {
            throw new InvalidTransactionException("Cannot transfer to the same account.");
        }

        Account destAcc = bank.getAccountMap().get(destAccNo);
        if (destAcc == null) {
            throw new InvalidTransactionException("Destination account " + destAccNo + " does not exist.");
        }

        if (destAcc.getStatus() != Account.AccountStatus.ACTIVE) {
            throw new InvalidTransactionException("Destination account is " + destAcc.getStatus() + ".");
        }

        if (destAcc.getAccountType() == Account.AccountType.FIXED_DEPOSIT) {
            throw new InvalidTransactionException("Fixed Deposit accounts do not accept incoming transfers.");
        }

        if (destAcc.getAccountType() == Account.AccountType.LOAN && amount > destAcc.getBalance()) {
            throw new InvalidTransactionException(
                String.format("Transfer amount exceeds remaining loan balance of Rs. %.2f.", destAcc.getBalance())
            );
        }

        return destAcc;
    }

    // Option 4: Transfer Funds (FR-12, FR-13, FR-21)
    private static void transfer_funds(Scanner sc, Bank bank, Customer customer) {
        // 1. Select source account
        Account sourceAcc = selectCustomerAccount(sc, bank, customer);
        if (sourceAcc == null) {
            return;
        }

        // 2. Ask for destination account number
        ConsoleUI.printPrompt("Enter Destination Account Number: ");
        int destAccNo;
        try {
            destAccNo = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            ConsoleUI.printError("Invalid input! Please enter a numeric Account Number.");
            return;
        }

        // 3. Ask for transfer amount
        ConsoleUI.printPrompt("Enter transfer amount: Rs. ");
        double amount;
        try {
            amount = Double.parseDouble(sc.nextLine().trim());
            if (!Double.isFinite(amount)) {
                ConsoleUI.printError("Invalid amount entered.");
                return;
            }
        } catch (NumberFormatException e) {
            ConsoleUI.printError("Invalid input! Please enter a valid numeric amount.");
            return;
        }

        // 4. All-or-Nothing Pre-Validation with Custom Exception (FR-12, FR-13, FR-21)
        Account destAcc;
        try {
            destAcc = validateTransferPreConditions(bank, sourceAcc, destAccNo, amount);
        } catch (InvalidTransactionException e) {
            sourceAcc.recordTransaction("Transfer-out", amount, destAccNo, "Failed", e.getMessage());
            BankStorage.saveData(bank);
            ConsoleUI.printError("Transfer failed! Reason: " + e.getMessage());
            return;
        }

        // 5. Execute the transfer (Debit source -> Credit destination)
        if (sourceAcc.transferOut(amount, destAccNo)) {
            destAcc.transferIn(amount, sourceAcc.getAccountNo());
            BankStorage.saveData(bank);

            Transaction lastTx = getLastTransaction(sourceAcc);
            ConsoleUI.printSuccess("Transfer successful! Transaction ID: "
                    + (lastTx != null ? lastTx.getTransactionId() : "N/A"));
            ConsoleUI.printInfo(String.format("Transferred Rs. %.2f from Account %d to Account %d.",
                    amount, sourceAcc.getAccountNo(), destAccNo));
            ConsoleUI.printInfo(String.format("New balance for Account %d: Rs. %.2f",
                    sourceAcc.getAccountNo(), sourceAcc.getBalance()));
        } else {
            BankStorage.saveData(bank);
            Transaction lastTx = getLastTransaction(sourceAcc);
            String reason = (lastTx != null) ? lastTx.getFailureReason() : "Transfer rejected";
            ConsoleUI.printError("Transfer failed! Reason: " + reason);
        }
    }

    // Option 5: View Recent Transactions (FR-17)
    private static void view_recent_transactions(Scanner sc, Bank bank, Customer customer) {
        Account selectedAcc = selectCustomerAccount(sc, bank, customer);
        if (selectedAcc == null) {
            return;
        }

        int count = 10; // Default to last 10 (FR-17)
        ConsoleUI.printPrompt("Enter number of recent transactions to view (Press Enter for default 10): ");
        String input = sc.nextLine().trim();

        if (!input.isEmpty()) {
            try {
                int parsed = Integer.parseInt(input);
                if (parsed <= 0) {
                    ConsoleUI.printWarning("Count must be positive. Using default of 10.");
                } else {
                    count = parsed;
                }
            } catch (NumberFormatException e) {
                ConsoleUI.printWarning("Invalid number entered. Using default of 10.");
            }
        }

        java.util.List<Transaction> history = selectedAcc.getTransactionHistory();
        if (history.isEmpty()) {
            ConsoleUI.printInfo("No transactions found for this account.");
            return;
        }

        int startIndex = Math.max(0, history.size() - count);
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
        List<String[]> rows = new ArrayList<>();
        for (int i = startIndex; i < history.size(); i++) {
            Transaction tx = history.get(i);
            rows.add(new String[]{
                    tx.getTimestamp().format(dtf),
                    tx.getTransactionId(),
                    tx.getType(),
                    String.format("%.2f", tx.getAmount()),
                    tx.getStatus(),
                    String.format("%.2f", tx.getResultingBalance()),
                    tx.getFailureReason()
            });
        }
        ConsoleUI.printTable(
                new String[]{"Date & Time", "TXN ID", "Type", "Amount (Rs.)", "Status",
                        "Balance (Rs.)", "Remarks / Reason"},
                rows,
                new int[]{19, 18, 14, 14, 9, 15, 32},
                4);
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
                ConsoleUI.printPrompt("Enter Start Date (dd-MM-yyyy): ");
                startDate = LocalDate.parse(sc.nextLine().trim(), dateFormatter);

                ConsoleUI.printPrompt("Enter End Date (dd-MM-yyyy): ");
                endDate = LocalDate.parse(sc.nextLine().trim(), dateFormatter);

                if (endDate.isBefore(startDate)) {
                    ConsoleUI.printError("End date cannot be earlier than start date! Try again.");
                    continue;
                }
                break;
            } catch (java.time.format.DateTimeParseException e) {
                ConsoleUI.printError("Invalid date format! Please use dd-MM-yyyy.");
            }
        }

        // Delegate statement generation to ReportService (SRP)
        String statementText = ReportService.generateAccountStatement(customer, selectedAcc, startDate, endDate);

        // 1. Print on screen (FR-18)
        System.out.print(statementText);

        // 2. Offer to save to a text file (FR-19)
        ConsoleUI.printPrompt("Would you like to save this statement to a text file? (y/n): ");
        String saveChoice = sc.nextLine().trim();
        if (saveChoice.equalsIgnoreCase("y")) {
            String fileName = "Statement_Acc" + selectedAcc.getAccountNo() + "_" + startDate + "_to_" + endDate + ".txt";
            java.nio.file.Path filePath = ReportService.saveReportToFile(fileName, statementText);
            if (filePath != null) {
                ConsoleUI.printSuccess("Statement saved successfully to: " + filePath.toAbsolutePath());
            }
        }
    }


    // Option 7: Open Additional Account OR Close Account / FD (FR-7, FR-15, FR-16)
    private static void manage_accounts(Scanner sc, Bank bank, Customer customer) {
        ConsoleUI.printMenuCard("ACCOUNT MANAGEMENT",
                "Open a New Additional Account",
                "Close an Existing Account / Redeem Fixed Deposit",
                "Cancel");
        ConsoleUI.printPrompt();

        int subChoice;
        try {
            subChoice = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            ConsoleUI.printError("Invalid choice.");
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
            ConsoleUI.printWarning("This account is already CLOSED.");
            return;
        }

        // Case A: Fixed Deposit Closure (FR-15 & FR-16)
        if (selectedAcc.getAccountType() == Account.AccountType.FIXED_DEPOSIT) {
            FixedDepositAccount fd = (FixedDepositAccount) selectedAcc;
            Account payoutAcc = bank.getAccountMap().get(fd.getLinkedSavingsAccountNo());

            if (payoutAcc == null || payoutAcc.getStatus() != Account.AccountStatus.ACTIVE) {
                ConsoleUI.printError("Cannot close FD: Linked payout Savings Account ("
                        + fd.getLinkedSavingsAccountNo() + ") is not Active.");
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
            ConsoleUI.printError(String.format(
                    "Cannot close Account %d! Balance must be Rs. 0.00 (Current balance: Rs. %.2f).",
                    selectedAcc.getAccountNo(), selectedAcc.getBalance()));
            ConsoleUI.printInfo("Please withdraw or transfer out the remaining funds first.");
            return;
        }

        selectedAcc.setStatus(Account.AccountStatus.CLOSED);
        BankStorage.saveData(bank);
        ConsoleUI.printSuccess("Account " + selectedAcc.getAccountNo()
            + " has been successfully CLOSED.");
    }

    public static void menu(Scanner sc, Bank bank) {
        // Step 1: Authenticate before showing the menu (FR-1)
        Customer loggedInCustomer = authenticateCustomer(sc, bank);
        if (loggedInCustomer == null) {
            return; // Go straight back to Main Menu if login failed
        }

        int choice;
        while (true) {
            ConsoleUI.printMenuHeader("MAIN MENU > CUSTOMER PORTAL | Logged in: "
                    + loggedInCustomer.getName() + " (ID: "
                    + loggedInCustomer.getCustomerID() + ")");
            ConsoleUI.printMenuCard("CUSTOMER ACTIONS",
                    "View Balance",
                    "Deposit",
                    "Withdraw",
                    "Transfer Funds",
                    "View Recent Transactions",
                    "Generate Statement",
                    "Open an Additional Account / Close Account",
                    "Logout");
            ConsoleUI.printPrompt();

            if (sc.hasNextInt()) {
                choice = sc.nextInt();
                sc.nextLine(); // <-- FIXED: Consume newline!
                switch (choice) {
                    case 1:
                        ConsoleUI.printSectionHeader("VIEW BALANCE");
                        view_balance(sc, bank, loggedInCustomer);
                        ConsoleUI.pauseForEnter(sc);
                        break;
                    case 2:
                        ConsoleUI.printSectionHeader("DEPOSIT");
                        deposit(sc, bank, loggedInCustomer);
                        ConsoleUI.pauseForEnter(sc);
                        break;
                    case 3:
                        ConsoleUI.printSectionHeader("WITHDRAW");
                        withdraw(sc, bank, loggedInCustomer);
                        ConsoleUI.pauseForEnter(sc);
                        break;
                    case 4:
                        ConsoleUI.printSectionHeader("TRANSFER FUNDS");
                        transfer_funds(sc, bank, loggedInCustomer);
                        ConsoleUI.pauseForEnter(sc);
                        break;
                    case 5:
                        ConsoleUI.printSectionHeader("VIEW RECENT TRANSACTIONS");
                        view_recent_transactions(sc, bank, loggedInCustomer);
                        ConsoleUI.pauseForEnter(sc);
                        break;
                    case 6:
                        ConsoleUI.printSectionHeader("GENERATE STATEMENT");
                        generate_statement(sc, bank, loggedInCustomer);
                        ConsoleUI.pauseForEnter(sc);
                        break;
                    case 7:
                        ConsoleUI.printSectionHeader("MANAGE ACCOUNTS");
                        manage_accounts(sc, bank, loggedInCustomer); // <-- FIXED: Hooked up Option 7!
                        ConsoleUI.pauseForEnter(sc);
                        break;
                    case 8:
                        ConsoleUI.printInfo("Logged out. Goodbye.");
                        return; // <-- FIXED: Changed break to return!
                    default:
                        ConsoleUI.printError("You entered an invalid choice. Please try again.");
                }
            } else {
                ConsoleUI.printError("Invalid input. Please try again.");
                sc.nextLine();
            }
        }
    }

}
