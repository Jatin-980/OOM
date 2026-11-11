package Banking_system.Code;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Map;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.IOException;

import Banking_system.Code.Account.AccountStatus;
import Banking_system.Code.Account.AccountType;


public class OperatorMenu {

    private static void printMenu(){
        ConsoleUI.printMenuHeader("MAIN MENU > BANK OPERATOR PORTAL");
        ConsoleUI.printMenuCard(
                "OPERATOR ACTIONS",
                "Create New Customer",
                "Open New Account for Existing Customer",
                "View All Customers and Accounts (read-only)",
                "Unlock a Customer Login",
                "Run Monthly Interest",
                "Export Summary Report (All Customers & Balances)",
                "Back to Main Menu");
        ConsoleUI.printPrompt();
    }

    private static void create_new_customer(Scanner sc , Bank bank){
        String name;
        LocalDate dobDate;
        String phoneNo;
        String emailId;
        String pin;

        while(true){
            ConsoleUI.printPrompt("Enter Name : ");
            name = sc.nextLine();
            if(name.trim().isEmpty()){
                ConsoleUI.printError("Name cannot be empty.");
                continue;
            }
            break;
            
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT);
        while(true){
            try {
                ConsoleUI.printPrompt("Enter DOB (dd-MM-yyyy) : ");
                String dobInput = sc.nextLine();
                dobDate = LocalDate.parse(dobInput , formatter);

                if(dobDate.isAfter(LocalDate.now())){
                    ConsoleUI.printError("Date of birth cannot be in future.");
                    continue;
                }
                
                if(dobDate.isBefore(LocalDate.of(1916, 9, 19))){
                    ConsoleUI.printError("Date of birth is unrealistic.");
                    continue;
                }

                int age = Period.between(dobDate, LocalDate.now()).getYears();
                if(age < 18){
                    ConsoleUI.printError("Age must be more than or equal to 18.");
                    continue;
                }
                break;
                
            }catch(DateTimeParseException e){
                ConsoleUI.printError("Invalid date: " + e.getMessage());
            }
            
        }
    
        while(true){
            ConsoleUI.printPrompt("Enter Phone number (10 digit) : ");
            String inptuPhoneNo = sc.nextLine();
            if(!inptuPhoneNo.matches("[0-9]{10}")){
                ConsoleUI.printError("Phone number must contain exactly 10 digits.");
                continue;
            }
            if(inptuPhoneNo.startsWith("0")){
                ConsoleUI.printError("Phone number cannot start with 0.");
                continue;
            }
            phoneNo = inptuPhoneNo;
            break;
        }

        while(true){
            ConsoleUI.printPrompt("Enter email ID : ");
            emailId = sc.nextLine();
            if (!emailId.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                ConsoleUI.printError("Invalid email. Please try again.");
                continue;
            }
            break;
        }


        while(true){
            ConsoleUI.printPrompt("Enter pin : ");
            String inputPin = sc.nextLine();
            if (!inputPin.matches("[0-9]{4}")) {
                ConsoleUI.printError("PIN must contain exactly 4 digits.");
                continue;
            }
            pin = inputPin;
            break;
        }

        Customer customer = new Customer(name, dobDate, phoneNo, emailId, pin);
        bank.addCustomer(customer);
        BankStorage.saveData(bank);
        ConsoleUI.printSuccess("Customer created successfully! Assigned Customer ID: "
                + customer.getCustomerID());
    }   

    private static void view_all_customers_and_accounts(Scanner sc , Bank bank){
        Map<Integer, Customer> customers = bank.getCustomersMap();
        ConsoleUI.printSectionHeader("CUSTOMER OVERVIEW");
        List<String[]> customerRows = new ArrayList<>();
        List<String[]> accountRows = new ArrayList<>();
        for (Customer customer : customers.values()) {
            customerRows.add(new String[]{
                    String.valueOf(customer.getCustomerID()),
                    customer.getName(),
                    String.valueOf(customer.getDobDate()),
                    customer.getPhoneNo(),
                    customer.getEmailId(),
                    String.valueOf(customer.getRegistrationDate()),
                    customer.isLocked() ? "Locked" : "Active"
            });
            for (int accNo : customer.getOwnedAccountList()) {
                Account acc = bank.getAccountMap().get(accNo);
                if (acc != null) {
                    accountRows.add(new String[]{
                            String.valueOf(acc.getAccountNo()),
                            String.valueOf(customer.getCustomerID()),
                            String.valueOf(acc.getAccountType()),
                            String.valueOf(acc.getStatus()),
                            String.valueOf(acc.getOpened()),
                            String.format("%.2f", acc.getBalance()),
                            acc.getSpecificDetails()
                    });
                }
            }
        }
        ConsoleUI.printTable(
                new String[]{"ID", "Name", "DOB", "Phone", "Email", "Reg Date", "Login Status"},
                customerRows,
                new int[]{8, 24, 12, 14, 30, 12, 14},
                -1);
        ConsoleUI.printSectionHeader("CUSTOMER ACCOUNTS");
        ConsoleUI.printTable(
                new String[]{"Acc No", "Owner ID", "Type", "Status", "Opened",
                        "Balance (Rs.)", "Details"},
                accountRows,
                new int[]{9, 10, 16, 12, 12, 17, 34},
                -1);
    }

    private static void open_new_account(Scanner sc, Bank bank) {
    int customerId;

    // 1. Validate customer ID
    while (true) {
        ConsoleUI.printPrompt("Enter Customer ID: ");
        try {
            customerId = Integer.parseInt(sc.nextLine().trim());

            if (!bank.findCustomer(customerId)) {
                ConsoleUI.printError("Customer not found.");
                ConsoleUI.printPrompt("Try again? (y/n): ");
                if (!sc.nextLine().trim().equalsIgnoreCase("y")) {
                    return;
                }
                continue;
            }
            break;
        } catch (NumberFormatException e) {
            ConsoleUI.printError("Enter a valid numeric customer ID.");
        }
    }
    Customer customer = bank.getCustomer(customerId);
    // Delegate the rest to the shared public method!
    openAccountForKnownCustomer(sc, bank, customer);
    }
    
    public static void openAccountForKnownCustomer(Scanner sc, Bank bank, Customer customer) {
        int customerId = customer.getCustomerID();
        int choice;

        // 2. Select account type
        while (true) {
            System.out.println("\n1. Savings\n2. Current\n3. Fixed Deposit\n4. Loan");
            ConsoleUI.printPrompt("Enter choice: ");

            try {
                choice = Integer.parseInt(sc.nextLine().trim());

                if (choice < 1 || choice > 4) {
                    ConsoleUI.printError("Choose 1, 2, 3, or 4.");
                    continue;
                }

                if (choice == 2) {
                    boolean hasCurrent = false;

                    for (int accNo : customer.getOwnedAccountList()) {
                        Account acc = bank.getAccountMap().get(accNo);

                        if (acc != null &&
                            acc.getAccountType() == AccountType.CURRENT &&
                            acc.getStatus() != AccountStatus.CLOSED) {
                            hasCurrent = true;
                            break;
                        }
                    }

                    if (hasCurrent) {
                        ConsoleUI.printError("Customer already has a Current Account.");
                        return;
                    }
                }

                break;
            } catch (NumberFormatException e) {
                ConsoleUI.printError("Enter a valid numeric choice.");
            }
        }

        AccountStatus status = AccountStatus.ACTIVE;
        LocalDate opened = LocalDate.now();
        Account newAccount;

        // Special creation flow for Loan accounts
        if (choice == 4) {
            double loanPrincipal;
            while (true) {
                ConsoleUI.printPrompt("Enter Loan Principal Amount: Rs. ");
                try {
                    loanPrincipal = Double.parseDouble(sc.nextLine().trim());
                    if (loanPrincipal <= 0) {
                        ConsoleUI.printError("Principal amount must be positive.");
                        continue;
                    }
                    break;
                } catch (NumberFormatException e) {
                    ConsoleUI.printError("Enter a valid numeric amount.");
                }
            }

            double interestRate;
            while (true) {
                ConsoleUI.printPrompt("Enter Annual Interest Rate (% e.g., 8.5): ");
                try {
                    interestRate = Double.parseDouble(sc.nextLine().trim());
                    if (interestRate <= 0 || interestRate > 100) {
                        ConsoleUI.printError("Enter a realistic interest rate between 0 and 100.");
                        continue;
                    }
                    break;
                } catch (NumberFormatException e) {
                    ConsoleUI.printError("Enter a valid number.");
                }
            }

            int loanDuration;
            while (true) {
                ConsoleUI.printPrompt("Enter Loan Duration in Months (e.g., 12, 24, 36, 60): ");
                try {
                    loanDuration = Integer.parseInt(sc.nextLine().trim());
                    if (loanDuration <= 0) {
                        ConsoleUI.printError("Duration must be at least 1 month.");
                        continue;
                    }
                    break;
                } catch (NumberFormatException e) {
                    ConsoleUI.printError("Enter a valid integer.");
                }
            }

            newAccount = AccountFactory.createNewLoan(
                customerId, loanPrincipal, status, opened,
                interestRate, loanDuration);
            bank.openAccountForCustomer(customerId, newAccount);
            newAccount.recordTransaction("Deposit", loanPrincipal, -1, "Success", "Loan principal disbursed");
            BankStorage.saveData(bank);
            ConsoleUI.printSuccess("Loan account created successfully. Account number: "
                    + newAccount.getAccountNo());
            return;
        }

        // 3. Validate initial deposit
        double initialDeposit;

        while (true) {
            ConsoleUI.printPrompt("Enter initial deposit: Rs. ");

            try {
                initialDeposit =
                    Double.parseDouble(sc.nextLine().trim());

                if (!Double.isFinite(initialDeposit) ||
                    initialDeposit < 0) {
                    ConsoleUI.printError("Enter a valid non-negative amount.");
                    continue;
                }

                if (choice == 1 &&
                    initialDeposit < SavingsAccount.getMinimumBalance()) {
                    ConsoleUI.printError("Minimum Savings balance is Rs. " +
                        SavingsAccount.getMinimumBalance());
                    continue;
                }

                // Replace these two references with the actual
                // opening-balance constants/getters in your Account classes.
                if (choice == 2 &&
                    initialDeposit < CurrentAccount.getMinimumBalance()) {
                    ConsoleUI.printError("Insufficient opening deposit.");
                    continue;
                }

                if (choice == 3 &&
                    initialDeposit < FixedDepositAccount.getMinimumBalance()) {
                    ConsoleUI.printError("Insufficient opening deposit.");
                    continue;
                }

                break;
            } catch (NumberFormatException e) {
                ConsoleUI.printError("Enter a valid amount.");
            }
        }

        // 4. Construct the appropriate subclass
        if (choice == 1) {
            newAccount = AccountFactory.createNewSavingsAccount(
                customerId, initialDeposit, status, opened);
            bank.openAccountForCustomer(customerId, newAccount);
                
        } else if (choice == 2) {
            newAccount = AccountFactory.createNewCurrentAccount(
                customerId, initialDeposit, status, opened);
            bank.openAccountForCustomer(customerId, newAccount);

        } else {
            // Find the customer's Savings accounts
            java.util.List<Integer> savingsAccounts =
                new java.util.ArrayList<>();

            for (int accNo : customer.getOwnedAccountList()) {
                Account acc = bank.getAccountMap().get(accNo);

                if (acc != null &&
                    acc.getAccountType() == AccountType.SAVINGS &&
                    acc.getStatus() == AccountStatus.ACTIVE) {
                    savingsAccounts.add(accNo);
                }
            }

            if (savingsAccounts.isEmpty()) {
                ConsoleUI.printError("Open an active Savings Account before creating an FD.");
                return;
            }

            int linkedSavingsAccountNo;

            while (true) {
                System.out.println("\nSelect Savings Account to link:");

                for (int i = 0; i < savingsAccounts.size(); i++) {
                    System.out.println((i + 1) + ". Account " +
                        savingsAccounts.get(i));
                }

                ConsoleUI.printPrompt("Enter choice: ");

                try {
                    int selected =
                        Integer.parseInt(sc.nextLine().trim());

                    if (selected >= 1 &&
                        selected <= savingsAccounts.size()) {
                        linkedSavingsAccountNo =
                            savingsAccounts.get(selected - 1);
                        break;
                    }

                    ConsoleUI.printError("Invalid choice.");
                } catch (NumberFormatException e) {
                    ConsoleUI.printError("Enter a valid number.");
                }
            }

            int tenureMonths;

            while (true) {
                ConsoleUI.printPrompt("Enter FD tenure (6, 12, 24, 36 months): ");

                try {
                    tenureMonths =
                        Integer.parseInt(sc.nextLine().trim());

                    if (tenureMonths == 6 || tenureMonths == 12 ||
                        tenureMonths == 24 || tenureMonths == 36) {
                        break;
                    }

                    ConsoleUI.printError("Invalid tenure.");
                } catch (NumberFormatException e) {
                    ConsoleUI.printError("Enter a valid number.");
                }
            }

            newAccount = AccountFactory.createNewFixedDepositAccount(
                customerId, initialDeposit, status, opened,
                linkedSavingsAccountNo, tenureMonths);
            bank.openAccountForCustomer(customerId, newAccount);
        }

        newAccount.recordTransaction("Deposit", initialDeposit, -1, "Success", "Opening deposit");
        BankStorage.saveData(bank);
        ConsoleUI.printSuccess("Account created successfully. Account number: "
                + newAccount.getAccountNo());
    }

    private static void unlock_a_customer_login(Scanner sc, Bank bank) {
        ConsoleUI.printPrompt("Enter Customer ID: ");
        int customerId;

        try {
            customerId = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            ConsoleUI.printError("Invalid input! Please enter a numeric Customer ID.");
            return;
        }

        if (!bank.findCustomer(customerId)) {
            ConsoleUI.printError("Customer with ID " + customerId + " not found.");
            return;
        }

        Customer customer = bank.getCustomer(customerId);

        if (customer.isLocked()) {
            customer.setLocked(false);
            customer.setFailedLoginAttempts(0); // Reset counter back to 0
            BankStorage.saveData(bank);         // Persist the unlocked state
            ConsoleUI.printSuccess("Customer " + customerId + " (" + customer.getName()
                    + ") has been unlocked.");
        } else {
            ConsoleUI.printInfo("Customer " + customerId + " (" + customer.getName()
                    + ") is already unlocked.");
        }
}

    private static void run_monthly_interest(Bank bank) {
        Map<Integer, Account> accounts = bank.getAccountMap();
        int processedCount = 0;
        int skippedAlreadyPaidCount = 0;

        for (Account acc : accounts.values()) {
            if (acc.getStatus() == AccountStatus.ACTIVE && acc instanceof MonthlyInterestBearing) {
                MonthlyInterestBearing interestBearingAcc = (MonthlyInterestBearing) acc;

                // Prevent processing interest twice in the same calendar month
                if (interestBearingAcc.hasReceivedInterestThisMonth()) {
                    skippedAlreadyPaidCount++;
                    continue;
                }

                String cycleResult = interestBearingAcc.processMonthlyInterestCycle();
                if (cycleResult != null && !cycleResult.isBlank()) {
                    processedCount++;
                    ConsoleUI.printInfo(cycleResult);
                }
            }
        }

        if (processedCount > 0) {
            BankStorage.saveData(bank);
            ConsoleUI.printSuccess(String.format(
                    "Monthly interest cycle complete. Processed %d interest-bearing account(s).",
                    processedCount));
        } else if (skippedAlreadyPaidCount > 0) {
            ConsoleUI.printInfo("Monthly interest has already been processed for all eligible "
                    + "interest-bearing accounts this month.");
        } else {
            ConsoleUI.printInfo("No eligible active interest-bearing accounts found for monthly calculation.");
        }
    }

    private static void export_summary_report(Scanner sc, Bank bank) {
        ReportService.generateAndExportBankSummaryReport(bank);
    }

    public static void menu(Scanner sc , Bank bank){
        int choice = -1;
        while(true){
            printMenu();

            if(sc.hasNextInt()){
                choice = sc.nextInt();
                sc.nextLine(); // Consume newline
                switch(choice){
                    case 1 :
                    ConsoleUI.printSectionHeader("CREATE NEW CUSTOMER");
                    create_new_customer(sc,bank);
                    ConsoleUI.pauseForEnter(sc);
                    break;
                case 2 :
                    ConsoleUI.printSectionHeader("OPEN NEW ACCOUNT");
                    open_new_account(sc , bank);
                    ConsoleUI.pauseForEnter(sc);
                    break;
                case 3 : 
                    ConsoleUI.printSectionHeader("VIEW CUSTOMERS AND ACCOUNTS");
                    view_all_customers_and_accounts(sc , bank);
                    ConsoleUI.pauseForEnter(sc);
                        break;
                case 4 : 
                    ConsoleUI.printSectionHeader("UNLOCK CUSTOMER LOGIN");
                    unlock_a_customer_login(sc , bank);
                    ConsoleUI.pauseForEnter(sc);
                    break;
                case 5 : 
                    ConsoleUI.printSectionHeader("RUN MONTHLY INTEREST");
                    run_monthly_interest(bank);
                    ConsoleUI.pauseForEnter(sc);
                    break;
                case 6 : 
                    ConsoleUI.printSectionHeader("EXPORT SUMMARY REPORT");
                    export_summary_report(sc, bank);
                    ConsoleUI.pauseForEnter(sc);
                    break;
                case 7 : 
                    ConsoleUI.printInfo("Returning to main menu.");
                    return;
                default : 
                    ConsoleUI.printError("You entered an invalid choice. Please try again.");
                }
               
            }
            else{
                ConsoleUI.printError("Invalid input. Please try again.");
                sc.nextLine();
            }



        }

    }
}
