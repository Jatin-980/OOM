package Banking_system.Code;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import Banking_system.Code.Account.AccountStatus;

/**
 * ReportService adheres to the Single Responsibility Principle (SRP).
 * It decouples financial statement calculation, executive summary generation,
 * and report file persistence from the presentation/CLI menu classes.
 */
public class ReportService {

    private static final Path DATA_DIR = Paths.get("data");

    // Private constructor prevents direct instantiation of utility/service class
    private ReportService() {}

    /**
     * Generates a detailed account statement for the given customer and account over a date range.
     *
     * @param customer  The account owner.
     * @param account   The target account.
     * @param startDate The statement period start date.
     * @param endDate   The statement period end date.
     * @return Formatted statement text.
     */
    public static String generateAccountStatement(Customer customer, Account account,
                                                  LocalDate startDate, LocalDate endDate) {
        List<Transaction> history = account.getTransactionHistory();
        List<Transaction> txInRange = new ArrayList<>();

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
                if (!openingSet && isSuccess) {
                    openingBalance = isCredit
                            ? (tx.getResultingBalance() - tx.getAmount())
                            : (tx.getResultingBalance() + tx.getAmount());
                    openingSet = true;
                }
            }
        }

        if (!openingSet) {
            if (!startDate.isBefore(account.getOpened())) {
                openingBalance = account.getBalance();
            } else {
                openingBalance = 0.0;
            }
        }

        double closingBalance = openingBalance + totalCredits - totalDebits;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        StringBuilder sb = new StringBuilder();
        sb.append("\n");
        appendCenteredTitle(sb, "ACCOUNT STATEMENT", 110);
        appendTable(sb,
                new String[]{"Statement Header", "Value"},
                List.of(
                        new String[]{"Customer Name", customer.getName()
                                + " (ID: " + customer.getCustomerID() + ")"},
                        new String[]{"Account", account.getAccountNo()
                                + " (" + account.getAccountType() + ")"},
                        new String[]{"Statement Period", startDate + " to " + endDate},
                        new String[]{"Opening Balance (Rs.)", String.format("%.2f", openingBalance)}
                ),
                new int[]{24, 72});

        List<String[]> transactionRows = new ArrayList<>();
        for (Transaction tx : txInRange) {
            transactionRows.add(new String[]{
                    tx.getTimestamp().format(dtf),
                    tx.getTransactionId(),
                    tx.getType(),
                    String.format("%.2f", tx.getAmount()),
                    tx.getStatus(),
                    String.format("%.2f", tx.getResultingBalance()),
                    tx.getFailureReason()
            });
        }
        sb.append("\nTRANSACTIONS\n");
        if (transactionRows.isEmpty()) {
            sb.append("+--------------------------------------------------------------------------------------------+\n");
            sb.append("| No transactions recorded during this period.                                               |\n");
            sb.append("+--------------------------------------------------------------------------------------------+\n");
        } else {
            appendTable(sb,
                    new String[]{"Date/Time", "TXN ID", "Type", "Amount (Rs.)", "Status",
                            "Resulting Balance (Rs.)", "Note"},
                    transactionRows,
                    new int[]{19, 14, 11, 12, 8, 22, 12});
        }

        sb.append("\nSUMMARY\n");
        appendTable(sb,
                new String[]{"Total Credits (Rs.)", "Total Debits (Rs.)", "Closing Balance (Rs.)"},
                Collections.singletonList(new String[]{
                        String.format("%.2f", totalCredits),
                        String.format("%.2f", totalDebits),
                        String.format("%.2f", closingBalance)
                }),
                new int[]{20, 20, 22});
        sb.append("END OF ACCOUNT STATEMENT\n");

        return sb.toString();
    }

    /**
     * Generates the executive bank summary report for all registered customers and accounts.
     *
     * @param bank The bank containing customer and account ledgers.
     * @return Formatted summary report text.
     */
    public static String generateBankSummaryReport(Bank bank) {
        Map<Integer, Customer> customers = bank.getCustomersMap();
        Map<Integer, Account> accounts = bank.getAccountMap();

        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");

        StringBuilder sb = new StringBuilder();
        appendCenteredTitle(sb, "BANK MANAGEMENT SYSTEM - MASTER SUMMARY REPORT", 110);
        appendTable(sb,
                new String[]{"Generated On", "Total Customers", "Total Accounts"},
                Collections.singletonList(new String[]{now.format(dtf), String.valueOf(customers.size()),
                        String.valueOf(accounts.size())}),
                new int[]{24, 18, 18});

        double totalSavings = 0.0;
        double totalCurrent = 0.0;
        double totalFD = 0.0;
        double totalLoan = 0.0;

        for (Account a : accounts.values()) {
            if (a.getStatus() == AccountStatus.ACTIVE) {
                switch (a.getAccountType()) {
                    case SAVINGS:
                        totalSavings += a.getBalance();
                        break;
                    case CURRENT:
                        totalCurrent += a.getBalance();
                        break;
                    case FIXED_DEPOSIT:
                        totalFD += a.getBalance();
                        break;
                    case LOAN:
                        totalLoan += a.getBalance();
                        break;
                }
            }
        }

        sb.append("\nFINANCIAL PORTFOLIO SUMMARY (ACTIVE ACCOUNTS)\n");
        appendTable(sb,
                new String[]{"Metric / Portfolio Category", "Amount (Rs.)"},
                List.of(
                        new String[]{"Total Savings Deposits", String.format("%,.2f", totalSavings)},
                        new String[]{"Total Current Balances", String.format("%,.2f", totalCurrent)},
                        new String[]{"Total Fixed Deposits", String.format("%,.2f", totalFD)},
                        new String[]{"Total Outstanding Loans", String.format("%,.2f", totalLoan)},
                        new String[]{"Net Bank Balance", String.format("%,.2f",
                                totalSavings + totalCurrent + totalFD - totalLoan)}
                ),
                new int[]{36, 22});
        sb.append("\nCUSTOMER & ACCOUNT DETAILS\n");

        if (customers.isEmpty()) {
            sb.append("+--------------------------------------------------------------------------------------------+\n");
            sb.append("| No customers found in system.                                                             |\n");
            sb.append("+--------------------------------------------------------------------------------------------+\n");
        } else {
            for (Customer c : customers.values()) {
                sb.append("\nCUSTOMER\n");
                appendTable(sb,
                        new String[]{"ID", "Name", "DOB", "Phone", "Email", "Reg Date", "Login Status"},
                        Collections.singletonList(new String[]{
                                String.valueOf(c.getCustomerID()),
                                c.getName(),
                                String.valueOf(c.getDobDate()),
                                c.getPhoneNo(),
                                c.getEmailId(),
                                String.valueOf(c.getRegistrationDate()),
                                c.isLocked() ? "LOCKED" : "Normal"
                        }),
                        new int[]{5, 18, 12, 12, 26, 12, 12});

                if (c.getOwnedAccountList().isEmpty()) {
                    sb.append("No accounts owned.\n");
                } else {
                    double customerTotalBal = 0.0;
                    List<String[]> accountRows = new ArrayList<>();
                    for (int accNo : c.getOwnedAccountList()) {
                        Account acc = accounts.get(accNo);
                        if (acc != null) {
                            accountRows.add(new String[]{
                                    String.valueOf(acc.getAccountNo()),
                                    String.valueOf(c.getCustomerID()),
                                    String.valueOf(acc.getAccountType()),
                                    String.valueOf(acc.getStatus()),
                                    String.valueOf(acc.getOpened()),
                                    String.format("%,.2f", acc.getBalance()),
                                    acc.getSpecificDetails()
                            });
                            if (acc.getStatus() == AccountStatus.ACTIVE) {
                                customerTotalBal += acc.getBalance();
                            }
                        }
                    }
                    appendTable(sb,
                            new String[]{"Acc No", "Owner ID", "Type", "Status", "Opened",
                                    "Balance (Rs.)", "Details"},
                            accountRows,
                            new int[]{8, 8, 15, 10, 12, 16, 30});
                    sb.append(String.format("Total Active Balance: Rs. %,.2f%n", customerTotalBal));
                }
            }
        }
        sb.append("\nEND OF MASTER SUMMARY REPORT\n");

        return sb.toString();
    }

    /**
     * Generates, displays on console, and exports the master bank summary report to a timestamped file.
     *
     * @param bank The bank containing customer and account ledgers.
     * @return Path to the created report file, or null on error.
     */
    public static Path generateAndExportBankSummaryReport(Bank bank) {
        String reportContent = generateBankSummaryReport(bank);

        // 1. Display report on console
        System.out.print(reportContent);

        // 2. Export to timestamped file in data directory
        String timestampStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss"));
        String fileName = "Bank_Summary_Report_" + timestampStr + ".txt";
        Path savedPath = saveReportToFile(fileName, reportContent);

        if (savedPath != null) {
            System.out.println("\nSummary report exported successfully to: " + savedPath.toAbsolutePath());
        } else {
            System.out.println("\nFailed to export summary report file.");
        }
        return savedPath;
    }

    /**
     * Saves a generated report's content string to a file inside the data directory.
     *
     * @param fileName File name within the data directory.
     * @param content  String content to persist.
     * @return Path to the saved file if successful, or null on error.
     */
    public static Path saveReportToFile(String fileName, String content) {
        Path filePath = DATA_DIR.resolve(fileName);
        try {
            if (filePath.getParent() != null) {
                Files.createDirectories(filePath.getParent());
            }
            Files.writeString(filePath, content);
            return filePath;
        } catch (IOException e) {
            System.out.println("Failed to save report file: " + e.getMessage());
            return null;
        }
    }

    private static void appendCenteredTitle(StringBuilder report, String title, int width) {
        String safeTitle = cleanCell(title);
        int padding = Math.max(0, (width - safeTitle.length()) / 2);
        report.append("=".repeat(width)).append('\n');
        report.append(" ".repeat(padding)).append(safeTitle).append('\n');
        report.append("=".repeat(width)).append('\n');
    }

    private static void appendTable(StringBuilder report, String[] headers,
                                    List<String[]> rows, int[] widths) {
        String border = tableBorder(widths);
        report.append(border).append('\n');
        appendTableRow(report, headers, widths);
        report.append(border).append('\n');
        for (String[] row : rows) {
            appendTableRow(report, row, widths);
        }
        report.append(border).append('\n');
    }

    private static String tableBorder(int[] widths) {
        StringBuilder border = new StringBuilder("+");
        for (int width : widths) {
            border.append("-".repeat(width + 2)).append("+");
        }
        return border.toString();
    }

    private static void appendTableRow(StringBuilder report, String[] values, int[] widths) {
        report.append('|');
        for (int i = 0; i < widths.length; i++) {
            String value = i < values.length ? cleanCell(values[i]) : "";
            value = truncate(value, widths[i]);
            report.append(' ')
                .append(String.format("%-" + widths[i] + "s", value))
                .append(" |");
        }
        report.append('\n');
    }

    private static String cleanCell(String value) {
        if (value == null) {
            return "";
        }
        return value.replaceAll("\\u001B\\[[;\\d]*m", "")
                .replaceAll("[\\r\\n\\t]", " ");
    }

    private static String truncate(String value, int width) {
        if (value.length() <= width) {
            return value;
        }
        if (width <= 3) {
            return value.substring(0, width);
        }
        return value.substring(0, width - 3) + "...";
    }
}
