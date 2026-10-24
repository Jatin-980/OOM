package Banking_system.Code;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import Banking_system.Code.Account.AccountStatus;
import Banking_system.Code.Account.AccountType;

public class BankStorage {

    private static final Path DATA_DIR = Paths.get("data");
    private static final Path CUSTOMER_FILE = DATA_DIR.resolve("customers.csv");
    private static final Path ACCOUNT_FILE = DATA_DIR.resolve("accounts.csv");
    private static final Path TRANSACTION_FILE = DATA_DIR.resolve("transactions.csv");

    // ==================== MASTER LOAD & SAVE ====================

    public static void loadData(Bank bank) {
        try {
            Files.createDirectories(DATA_DIR);
        } catch (IOException e) {
            System.err.println("Error creating data directory: " + e.getMessage());
            return;
        }

        // 1. Load Customers first
        Map<Integer, Customer> customers = loadCustomers();
        bank.setCustomersMap(customers);

        // 2. Load Accounts and link them to their Customer's ownedAccountList
        Map<Integer, Account> accounts = loadAccounts(customers);
        bank.setAccountMap(accounts);

        // 3. Load Transactions and attach them to their Account's transactionHistory
        loadTransactions(accounts);
    }

    public static void saveData(Bank bank) {
        try {
            Files.createDirectories(DATA_DIR);
            saveCustomers(bank.getCustomersMap());
            saveAccounts(bank.getAccountMap());
            saveTransactions(bank.getAccountMap());
        } catch (IOException e) {
            System.err.println("Error initializing storage directory: " + e.getMessage());
        }
    }

    // ==================== 1. CUSTOMERS ====================

    private static Map<Integer, Customer> loadCustomers() {
        Map<Integer, Customer> customers = new HashMap<>();
        if (!Files.exists(CUSTOMER_FILE)) {
            return customers;
        }

        try (BufferedReader br = Files.newBufferedReader(CUSTOMER_FILE)) {
            String line = br.readLine(); // Skip CSV header

            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;

                String[] fields = line.split(",", -1);
                if (fields.length < 7) {
                    System.err.println("Invalid customer CSV record: " + line);
                    continue;
                }

                int id = Integer.parseInt(fields[0]);
                String name = fields[1];
                LocalDate dob = LocalDate.parse(fields[2]);
                String phone = fields[3];
                String email = fields[4];
                String pin = fields[5];
                LocalDate registrationDate = LocalDate.parse(fields[6]);

                Customer customer = new Customer(id, name, dob, phone, email, pin, registrationDate);

                // Support lockout columns if present
                if (fields.length >= 9) {
                    customer.setFailedLoginAttempts(Integer.parseInt(fields[7]));
                    customer.setLocked(Boolean.parseBoolean(fields[8]));
                }

                customers.put(id, customer);
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("Error loading customers: " + e.getMessage());
        }
        return customers;
    }

    private static void saveCustomers(Map<Integer, Customer> customers) {
        try (BufferedWriter bw = Files.newBufferedWriter(CUSTOMER_FILE)) {
            bw.write("customerID,name,dob,phoneNo,emailId,pin,registrationDate,failedLoginAttempts,isLocked");
            bw.newLine();

            for (Customer c : customers.values()) {
                bw.write(
                    c.getCustomerID() + "," +
                    sanitizeCsv(c.getName()) + "," +
                    c.getDobDate() + "," +
                    c.getPhoneNo() + "," +
                    sanitizeCsv(c.getEmailId()) + "," +
                    c.getPin() + "," +
                    c.getRegistrationDate() + "," +
                    c.getFailedLoginAttempts() + "," +
                    c.isLocked()
                );
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving customers: " + e.getMessage());
        }
    }

    // ==================== 2. ACCOUNTS ====================

    private static Map<Integer, Account> loadAccounts(Map<Integer, Customer> customers) {
        Map<Integer, Account> accounts = new HashMap<>();
        if (!Files.exists(ACCOUNT_FILE)) {
            return accounts;
        }

        try (BufferedReader br = Files.newBufferedReader(ACCOUNT_FILE)) {
            String line = br.readLine(); // Skip CSV header

            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;

                String[] fields = line.split(",", -1);
                if (fields.length < 8) {
                    System.err.println("Invalid account CSV record: " + line);
                    continue;
                }

                int accountNo = Integer.parseInt(fields[0]);
                int customerId = Integer.parseInt(fields[1]);
                AccountType type = AccountType.valueOf(fields[2]);
                double balance = Double.parseDouble(fields[3]);
                AccountStatus status = AccountStatus.valueOf(fields[4]);
                LocalDate opened = LocalDate.parse(fields[5]);

                Account account = null;

                switch (type) {
                    case SAVINGS:
                        account = new SavingsAccount(accountNo, customerId, balance, status, opened);
                        break;
                    case CURRENT:
                        account = new CurrentAccount(accountNo, customerId, balance, status, opened);
                        break;
                    case FIXED_DEPOSIT:
                        int linkedSavings = Integer.parseInt(fields[6]);
                        int tenureMonths = Integer.parseInt(fields[7]);
                        account = new FixedDepositAccount(
                            accountNo, customerId, balance, status, opened, linkedSavings, tenureMonths
                        );
                        break;
                }

                if (account != null) {
                    accounts.put(accountNo, account);

                    // Rebuild the Customer's ownedAccountList automatically
                    Customer owner = customers.get(customerId);
                    if (owner != null) {
                        owner.addAccountNo(accountNo);
                    }
                }
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("Error loading accounts: " + e.getMessage());
        }
        return accounts;
    }

    private static void saveAccounts(Map<Integer, Account> accounts) {
        try (BufferedWriter bw = Files.newBufferedWriter(ACCOUNT_FILE)) {
            bw.write("accountNo,customerId,accountType,balance,status,openedDate,linkedSavingsAccountNo,tenureMonths");
            bw.newLine();

            for (Account acc : accounts.values()) {
                int linkedSavings = -1;
                int tenureMonths = 0;

                if (acc instanceof FixedDepositAccount) {
                    FixedDepositAccount fd = (FixedDepositAccount) acc;
                    linkedSavings = fd.getLinkedSavingsAccountNo();
                    tenureMonths = fd.getTenureMonths();
                }

                bw.write(
                    acc.getAccountNo() + "," +
                    acc.getCustomerId() + "," +
                    acc.getAccountType() + "," +
                    acc.getBalance() + "," +
                    acc.getStatus() + "," +
                    acc.getOpened() + "," +
                    linkedSavings + "," +
                    tenureMonths
                );
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error saving accounts: " + e.getMessage());
        }
    }

    // ==================== 3. TRANSACTIONS ====================

    private static void loadTransactions(Map<Integer, Account> accounts) {
        if (!Files.exists(TRANSACTION_FILE)) {
            return;
        }

        try (BufferedReader br = Files.newBufferedReader(TRANSACTION_FILE)) {
            String line = br.readLine(); // Skip CSV header

            while ((line = br.readLine()) != null) {
                if (line.isBlank()) continue;

                String[] fields = line.split(",", -1);
                if (fields.length < 10) {
                    System.err.println("Invalid transaction CSV record: " + line);
                    continue;
                }

                int ledgerAccountNo = Integer.parseInt(fields[0]);
                String txId = fields[1];
                LocalDateTime timestamp = LocalDateTime.parse(fields[2]);
                String type = fields[3];
                double amount = Double.parseDouble(fields[4]);
                int sourceAccount = Integer.parseInt(fields[5]);
                int destAccount = Integer.parseInt(fields[6]);
                String status = fields[7];
                String reason = fields[8];
                double resultingBalance = Double.parseDouble(fields[9]);

                Transaction tx = new Transaction(
                    txId, timestamp, type, amount, sourceAccount, destAccount, status, reason, resultingBalance
                );

                // Attach transaction back to the specific account's history
                Account account = accounts.get(ledgerAccountNo);
                if (account != null) {
                    account.getTransactionHistory().add(tx);
                }
            }
        } catch (IOException | RuntimeException e) {
            System.err.println("Error loading transactions: " + e.getMessage());
        }
    }

    private static void saveTransactions(Map<Integer, Account> accounts) {
        try (BufferedWriter bw = Files.newBufferedWriter(TRANSACTION_FILE)) {
            bw.write("ledgerAccountNo,transactionId,timestamp,type,amount,sourceAccount,destinationAccount,status,failureReason,resultingBalance");
            bw.newLine();

            for (Account acc : accounts.values()) {
                for (Transaction tx : acc.getTransactionHistory()) {
                    bw.write(
                        acc.getAccountNo() + "," +
                        tx.getTransactionId() + "," +
                        tx.getTimestamp() + "," +
                        sanitizeCsv(tx.getType()) + "," +
                        tx.getAmount() + "," +
                        tx.getSourceAccount() + "," +
                        tx.getDestinationAccount() + "," +
                        sanitizeCsv(tx.getStatus()) + "," +
                        sanitizeCsv(tx.getFailureReason()) + "," +
                        tx.getResultingBalance()
                    );
                    bw.newLine();
                }
            }
        } catch (IOException e) {
            System.err.println("Error saving transactions: " + e.getMessage());
        }
    }

    // Prevents commas in user names or failure reasons from breaking CSV column alignment
    private static String sanitizeCsv(String value) {
        if (value == null) return "None";
        return value.replace(",", ";");
    }
}