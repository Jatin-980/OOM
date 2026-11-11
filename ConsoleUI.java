package Banking_system.Code;

import java.util.Scanner;
import java.util.List;

/**
 * Shared presentation helpers for the banking CLI.
 */
public final class ConsoleUI {

    public static final String RESET = "\u001B[0m";
    public static final String BOLD = "\u001B[1m";
    public static final String DIM = "\u001B[2m";
    public static final String GREEN = "\u001B[92m";
    public static final String YELLOW = "\u001B[93m";
    public static final String RED = "\u001B[91m";
    public static final String BORDER = "\u001B[38;5;245m";
    public static final String HEADING = BOLD + "\u001B[38;5;230m";
    public static final String OPTION_BADGE = "\u001B[38;5;222m";
    public static final String OPTION_TEXT = "\u001B[38;5;252m";

    private static final int BOX_WIDTH = 68;

    private ConsoleUI() {
    }

    public static void printAppBanner() {
        String title = "CLI BANKING ENGINE";
        printBoxedTitle(title, HEADING);
    }

    public static void printMenuHeader(String breadcrumbTitle) {
        System.out.println();
        printBoxedTitle(breadcrumbTitle, HEADING);
        System.out.println();
    }

    public static void printSectionHeader(String title) {
        System.out.println();
        System.out.println(HEADING + "--- [ " + title + " ] ---" + RESET);
    }

    public static void printMenuCard(String title, String... options) {
        String border = BORDER;
        System.out.println(border + "╔" + "═".repeat(BOX_WIDTH) + "╗" + RESET);
        printMenuRow(" " + HEADING + title + RESET);
        System.out.println(border + "╠" + "═".repeat(BOX_WIDTH) + "╣" + RESET);
        for (int i = 0; i < options.length; i++) {
            String option = " " + OPTION_BADGE + BOLD + "[" + (i + 1) + "]"
                    + RESET + " " + OPTION_TEXT + options[i] + RESET;
            printMenuRow(option);
        }
        System.out.println(border + "╚" + "═".repeat(BOX_WIDTH) + "╝" + RESET);
    }

    public static void printPrompt() {
        printPrompt("➤ Enter your choice: ");
    }

    public static void printPrompt(String label) {
        System.out.print(HEADING + label + RESET);
    }

    public static void printDivider(int width) {
        if (width <= 0) {
            throw new IllegalArgumentException("Divider width must be greater than zero.");
        }
        System.out.println(DIM + "-".repeat(width) + RESET);
    }

    public static void printSuccess(String msg) {
        printStatus(GREEN, "[✔] ", msg);
    }

    public static void printError(String msg) {
        printStatus(RED, "[✖] ", msg);
    }

    public static void printWarning(String msg) {
        printStatus(YELLOW, "[!] ", msg);
    }

    public static void printInfo(String msg) {
        printStatus(HEADING, "[i] ", msg);
    }

    public static void printTable(String[] headers, List<String[]> rows,
                                  int[] maximumWidths, int statusColumn) {
        int[] widths = new int[headers.length];
        for (int i = 0; i < headers.length; i++) {
            widths[i] = Math.min(headers[i].length(), maximumWidths[i]);
        }
        for (String[] row : rows) {
            for (int i = 0; i < headers.length; i++) {
                widths[i] = Math.min(maximumWidths[i],
                        Math.max(widths[i], displayValue(row, i).length()));
            }
        }

        String border = buildTableBorder(widths);
        printTableBorder(border);
        printTableRow(headers, widths, HEADING, -1);
        printTableBorder(border);
        for (String[] row : rows) {
            int rowColor = statusColumn >= 0 && statusColumn < row.length
                    ? statusColor(row[statusColumn]) : -1;
            printTableRow(row, widths, null, rowColor, statusColumn);
        }
        printTableBorder(border);
    }

    public static void pauseForEnter(Scanner sc) {
        System.out.print(DIM + "\nPress [ENTER] to return to menu..." + RESET);
        sc.nextLine();
    }

    public static int visibleLength(String str) {
        return str.replaceAll("\\u001B\\[[;\\d]*m", "").length();
    }

    public static String padRightVisible(String str, int targetVisibleWidth) {
        int padding = Math.max(0, targetVisibleWidth - visibleLength(str));
        return str + " ".repeat(padding);
    }

    private static void printBoxedTitle(String title, String style) {
        String content = " " + title + " ";
        int innerWidth = BOX_WIDTH;
        if (content.length() > innerWidth) {
            content = content.substring(0, innerWidth);
        }

        int leftPadding = (innerWidth - content.length()) / 2;
        int rightPadding = innerWidth - content.length() - leftPadding;

        String titleRow = style + " ".repeat(leftPadding) + content
                + " ".repeat(rightPadding) + RESET;
        System.out.println(BORDER + "╔" + "═".repeat(innerWidth) + "╗" + RESET);
        System.out.println("║" + padRightVisible(titleRow, innerWidth) + "║");
        System.out.println(BORDER + "╚" + "═".repeat(innerWidth) + "╝" + RESET);
    }

    private static void printStatus(String color, String prefix, String msg) {
        System.out.println(color + prefix + msg + RESET);
    }

    private static String buildTableBorder(int[] widths) {
        StringBuilder border = new StringBuilder("+");
        for (int width : widths) {
            border.append("-".repeat(width + 2)).append("+");
        }
        return border.toString();
    }

    private static void printTableRow(String[] values, int[] widths,
                                      String rowStyle, int rowColor) {
        printTableRow(values, widths, rowStyle, rowColor, -1);
    }

    private static void printTableRow(String[] values, int[] widths,
                                      String rowStyle, int rowColor, int statusColumn) {
        StringBuilder row = new StringBuilder("|");
        for (int i = 0; i < widths.length; i++) {
            String value = truncate(displayValue(values, i), widths[i]);
            String padded = String.format(" %-" + widths[i] + "s ", value);
            if (rowColor >= 0 && i == statusColumn) {
                padded = colorFor(rowColor) + padded + RESET;
            }
            row.append(padded).append("|");
        }
        String rendered = row.toString();
        if (rowStyle != null) {
            rendered = rowStyle + rendered + RESET;
        }
        System.out.println(rendered);
    }

    private static String displayValue(String[] values, int index) {
        return index < values.length && values[index] != null ? values[index] : "";
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

    private static int statusColor(String status) {
        if ("Success".equalsIgnoreCase(status)) {
            return 0;
        }
        if ("Failed".equalsIgnoreCase(status)) {
            return 1;
        }
        return -1;
    }

    private static String colorFor(int color) {
        return color == 0 ? GREEN : RED;
    }

    private static void printMenuRow(String content) {
        System.out.println(BORDER + "║" + padRightVisible(content, BOX_WIDTH)
                + "║" + RESET);
    }

    private static void printTableBorder(String border) {
        System.out.println(BORDER + border + RESET);
    }
}