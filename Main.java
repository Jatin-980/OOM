package Banking_system.Code;
import java.util.Scanner;


public class Main {    
    public static void main(String[] args) {
        

        Scanner sc = new Scanner(System.in);
        Bank bank = new Bank();
        BankStorage.loadData(bank);
        int choice = -1;

        while(true){

            System.out.println("===============MAIN MENU===============");
            System.out.println("1. Bank Operator Menu ");
            System.out.println("2. Customer Login     ");
            System.out.println("3. Exit               ");
            System.out.print("Enter your choice : ");

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
                    System.out.println("Saving data...");
                    // Save data on exit
                    BankStorage.saveData(bank);
                    System.out.println("Exiting system !!");
                    break;
                }
                else{
                    System.out.println("You Entered wrong choice , try again !!");
                }
            }
            else{
                System.out.println("Invalid input , try again !!");
                sc.nextLine();
            }
        }


        sc.close();

    }
}


