package Banking_system.Code;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;
import java.util.Map;
import java.util.Scanner;

import Banking_system.Code.Account.AccountStatus;
import Banking_system.Code.Account.AccountType;


public class OperatorMenu {

    private static void printMenu(){
        System.out.println("1. Create New Customer");
        System.out.println("2. Open New Account for Existing Customer");
        System.out.println("3. View All Customers and Accounts (read-only)");
        System.out.println("4. Unlock a Customer Login");
        System.out.println("5. Run Monthly Interest");
        System.out.println("6. Back to Main Menu");
        System.out.print("Enter your choise : ");
    }

    private static void create_new_customer(Scanner sc , Bank bank){
        String name;
        LocalDate dobDate;
        String phoneNo;
        String emailId;
        String pin;

        while(true){
            System.out.println("Enter Name : ");
            name = sc.nextLine();
            if(name.trim().isEmpty()){
                System.out.println("Name cannot be emtpy !");
                continue;
            }
            break;
            
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-uuuu").withResolverStyle(ResolverStyle.STRICT);
        while(true){
            try {
                System.out.println("Enter DOB (dd-MM-yyyy) : ");
                String dobInput = sc.nextLine();
                dobDate = LocalDate.parse(dobInput , formatter);

                if(dobDate.isAfter(LocalDate.now())){
                    System.out.println("Date of birth cannot be in future !!");
                    continue;
                }
                
                if(dobDate.isBefore(LocalDate.of(1916, 9, 19))){
                    System.out.println("Date of birth is unrealistic ");
                    continue;
                }

                int age = Period.between(dobDate, LocalDate.now()).getYears();
                if(age < 18){
                    System.out.println("Age must be more than or equal to 18 !!");
                    continue;
                }
                break;
                
            }catch(DateTimeParseException e){
                System.out.println("Error : " + e.getMessage());
            }
            
        }
    
        while(true){
            System.out.println("Enter Phone number (10 digit) : ");
            String inptuPhoneNo = sc.nextLine();
            if(!inptuPhoneNo.matches("[0-9]{10}")){
                System.out.println("Phone number must contain exactly 10 digits!");
                continue;
            }
            if(inptuPhoneNo.startsWith("0")){
                System.out.println("Phone no cannot start with 0");
                continue;
            }
            phoneNo = inptuPhoneNo;
            break;
        }

        while(true){
            System.out.println("Enter email ID : ");
            emailId = sc.nextLine();
            if (!emailId.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")) {
                System.out.println("Invalid email, try again!");
                continue;
            }
            break;
        }


        while(true){
            System.out.println("Enter pin : ");
            String inputPin = sc.nextLine();
            if (!inputPin.matches("[0-9]{4}")) {
                System.out.println("PIN must contain exactly 4 digits!");
                continue;
            }
            pin = inputPin;
            break;
        }

        Customer customer = new Customer(name, dobDate, phoneNo, emailId, pin);
        bank.addCustomer(customer);
        BankStorage.saveData(bank);
        System.out.println("Customer created successfully! Assigned Customer ID: " + customer.getCustomerID());
    }   

    private static void view_all_customers_and_accounts(Scanner sc , Bank bank){
        Map<Integer, Customer> customers = bank.getCustomersMap();
        customers.forEach((id, customer) ->{
            System.out.println("\n========== CUSTOMER DETAILS ==========");
            System.out.println("Customer ID       : " + customer.getCustomerID());
            System.out.println("Name              : " + customer.getName());
            System.out.println("Date of Birth     : " + customer.getDobDate());
            System.out.println("Registration Date  : " + customer.getRegistrationDate());
            System.out.println("Phone Number      : " + customer.getPhoneNo());
            System.out.println("Email ID          : " + customer.getEmailId());
            System.out.println("Owned Accounts    : " + customer.getOwnedAccountList());
            for (int accNo : customer.getOwnedAccountList()) {
                Account acc = bank.getAccountMap().get(accNo);
                if (acc != null) {
                    System.out.printf("   -> Account %d | Type: %s | Balance: Rs. %.2f | Status: %s%n",
                        acc.getAccountNo(), acc.getAccountType(), acc.getBalance(), acc.getStatus());
                }
            }
            System.out.println("=======================================\n");
            }
        );
    }

    private static void open_new_account(Scanner sc, Bank bank) {
    int customerId;

    // 1. Validate customer ID
    while (true) {
        System.out.print("Enter Customer ID: ");
        try {
            customerId = Integer.parseInt(sc.nextLine().trim());

            if (!bank.findCustomer(customerId)) {
                System.out.println("Customer not found.");
                System.out.print("Try again? (y/n): ");
                if (!sc.nextLine().trim().equalsIgnoreCase("y")) {
                    return;
                }
                continue;
            }
            break;
        } catch (NumberFormatException e) {
            System.out.println("Enter a valid numeric customer ID.");
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
            System.out.println("\n1. Savings\n2. Current\n3. Fixed Deposit");
            System.out.print("Enter choice: ");

            try {
                choice = Integer.parseInt(sc.nextLine().trim());

                if (choice < 1 || choice > 3) {
                    System.out.println("Choose 1, 2, or 3.");
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
                        System.out.println(
                            "Customer already has a Current Account.");
                        return;
                    }
                }

                break;
            } catch (NumberFormatException e) {
                System.out.println("Enter a valid numeric choice.");
            }
        }

        // 3. Validate initial deposit
        double initialDeposit;

        while (true) {
            System.out.print("Enter initial deposit: Rs. ");

            try {
                initialDeposit =
                    Double.parseDouble(sc.nextLine().trim());

                if (!Double.isFinite(initialDeposit) ||
                    initialDeposit < 0) {
                    System.out.println("Enter a valid non-negative amount.");
                    continue;
                }

                if (choice == 1 &&
                    initialDeposit < SavingsAccount.getMinimumBalance()) {
                    System.out.println("Minimum Savings balance is Rs. " +
                        SavingsAccount.getMinimumBalance());
                    continue;
                }

                // Replace these two references with the actual
                // opening-balance constants/getters in your Account classes.
                if (choice == 2 &&
                    initialDeposit < CurrentAccount.getMinimumBalance()) {
                    System.out.println("Insufficient opening deposit.");
                    continue;
                }

                if (choice == 3 &&
                    initialDeposit < FixedDepositAccount.getMinimumBalance()) {
                    System.out.println("Insufficient opening deposit.");
                    continue;
                }

                break;
            } catch (NumberFormatException e) {
                System.out.println("Enter a valid amount.");
            }
        }

        AccountStatus status = AccountStatus.ACTIVE;
        LocalDate opened = LocalDate.now();
        Account newAccount;

        // 4. Construct the appropriate subclass
        if (choice == 1) {
            newAccount = new SavingsAccount(
                -1, customerId,
                initialDeposit, status, opened);
                bank.openAccountForCustomer(customerId, newAccount);
                
            } else if (choice == 2) {
                newAccount = new CurrentAccount(
                    -1, customerId,
                    initialDeposit, status, opened);
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
                System.out.println(
                    "Open an active Savings Account before creating an FD.");
                return;
            }

            int linkedSavingsAccountNo;

            while (true) {
                System.out.println("\nSelect Savings Account to link:");

                for (int i = 0; i < savingsAccounts.size(); i++) {
                    System.out.println((i + 1) + ". Account " +
                        savingsAccounts.get(i));
                }

                System.out.print("Enter choice: ");

                try {
                    int selected =
                        Integer.parseInt(sc.nextLine().trim());

                    if (selected >= 1 &&
                        selected <= savingsAccounts.size()) {
                        linkedSavingsAccountNo =
                            savingsAccounts.get(selected - 1);
                        break;
                    }

                    System.out.println("Invalid choice.");
                } catch (NumberFormatException e) {
                    System.out.println("Enter a valid number.");
                }
            }

            int tenureMonths;

            while (true) {
                System.out.print("Enter FD tenure (6, 12, 24, 36 months): ");

                try {
                    tenureMonths =
                        Integer.parseInt(sc.nextLine().trim());

                    if (tenureMonths == 6 || tenureMonths == 12 ||
                        tenureMonths == 24 || tenureMonths == 36) {
                        break;
                    }

                    System.out.println("Invalid tenure.");
                } catch (NumberFormatException e) {
                    System.out.println("Enter a valid number.");
                }
            }

            newAccount = new FixedDepositAccount(
                -1, customerId,
                initialDeposit, status, opened,
                linkedSavingsAccountNo, tenureMonths);
                bank.openAccountForCustomer(customerId, newAccount);
        }

        newAccount.recordTransaction("Deposit", initialDeposit, -1, "Success", "Opening deposit");
        BankStorage.saveData(bank);
        System.out.println("Account created successfully. Account number: " + newAccount.getAccountNo());
    }

    private static void unlock_a_customer_login(Scanner sc, Bank bank) {
        System.out.print("Enter Customer ID: ");
        int customerId;

        try {
            customerId = Integer.parseInt(sc.nextLine().trim());
        } catch (NumberFormatException e) {
            System.out.println("Invalid input! Please enter a numeric Customer ID.");
            return;
        }

        if (!bank.findCustomer(customerId)) {
            System.out.println("Customer with ID " + customerId + " not found.");
            return;
        }

        Customer customer = bank.getCustomer(customerId);

        if (customer.isLocked()) {
            customer.setLocked(false);
            customer.setFailedLoginAttempts(0); // Reset counter back to 0
            BankStorage.saveData(bank);         // Persist the unlocked state
            System.out.println("Done! Customer " + customerId + " (" + customer.getName() + ") has been unlocked.");
        } else {
            System.out.println("Customer " + customerId + " (" + customer.getName() + ") is already unlocked.");
        }
}

    private static void run_monthly_interest(Bank bank) {
        Map<Integer, Account> accounts = bank.getAccountMap();
        int creditedCount = 0;
        int skippedAlreadyPaidCount = 0;
        double totalInterestPaid = 0.0;

        for (Account acc : accounts.values()) {
            // Only process ACTIVE Savings accounts (FR-14)
            if (acc.getAccountType() == AccountType.SAVINGS &&
                acc.getStatus() == AccountStatus.ACTIVE) {

                SavingsAccount savingsAcc = (SavingsAccount) acc;

                // Prevent paying interest twice in the same month (Section 10)
                if (savingsAcc.hasReceivedInterestThisMonth()) {
                    skippedAlreadyPaidCount++;
                    continue;
                }

                double lowestBal = savingsAcc.getLowestBalanceThisMonth();
                double interest = savingsAcc.creditMonthlyInterest();

                if (interest > 0) {
                    creditedCount++;
                    totalInterestPaid += interest;
                    System.out.printf(
                        "Account %d: Lowest Monthly Balance = Rs. %.2f | Interest Credited = Rs. %.2f | New Balance = Rs. %.2f%n",
                        savingsAcc.getAccountNo(), lowestBal, interest, savingsAcc.getBalance()
                    );
                }
            }
        }

        if (creditedCount > 0) {
            BankStorage.saveData(bank);
            System.out.printf("Monthly interest cycle complete. Credited %d account(s) with a total of Rs. %.2f.%n",
                    creditedCount, totalInterestPaid);
        } else if (skippedAlreadyPaidCount > 0) {
            System.out.println("Monthly interest has already been credited for all eligible Savings accounts this month.");
        } else {
            System.out.println("No eligible active Savings accounts found for interest calculation.");
        }
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
                        create_new_customer(sc,bank);
                        break;
                    case 2 :
                        open_new_account(sc , bank);
                        break;
                    case 3 : 
                        view_all_customers_and_accounts(sc , bank);
                        break;
                    case 4 : 
                        unlock_a_customer_login(sc , bank);
                        break;
                    case 5 : 
                        run_monthly_interest(bank);
                        break;
                    case 6 : 
                        System.out.println("Exiting to main menu !!");
                        return;
                    default : 
                        System.out.println("You Entered wrong choice , try again !!");
                }
               
            }
            else{
                System.out.println("Invalid input , try again !!");
                sc.next();
            }



        }

    }
}
