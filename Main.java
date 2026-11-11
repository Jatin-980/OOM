package Banking_system.Code;
import java.util.Scanner;


public class Main {    
    public static void main(String[] args) {
        

        Scanner sc = new Scanner(System.in);
        Bank bank = Bank.getInstance();
        BankStorage.loadData(bank);
        int choice = -1;

        ConsoleUI.printAppBanner();
        while(true){
            ConsoleUI.printMenuHeader("MAIN MENU");
            ConsoleUI.printMenuCard("BANKING ENGINE", "Bank Operator Menu", "Customer Login", "Exit");
            ConsoleUI.printPrompt();

            if(sc.hasNextInt()){
                choice = sc.nextInt();
                sc.nextLine();
                if(choice==1){
                    OperatorMenu.menu(sc, bank);
                }
                else if(choice==2){
                    CustomerMenu.menu(sc , bank);
                    
                }
                else if(choice==3){
                    ConsoleUI.printInfo("Saving data...");
                    // Save data on exit
                    BankStorage.saveData(bank);
                    ConsoleUI.printSuccess("Exiting system.");
                    break;
                }
                else{
                    ConsoleUI.printError("You entered an invalid choice. Please try again.");
                }
            }
            else{
                ConsoleUI.printError("Invalid input. Please try again.");
                sc.nextLine();
            }
        }


        sc.close();

    }
}

