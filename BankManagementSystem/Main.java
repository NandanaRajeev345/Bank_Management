import java.util.Scanner;

public class Main {

    public static void main(String[] args) {

        Scanner sc = new Scanner(System.in);
        BankService bank = new BankService();

        while (true) {

            System.out.println("\n==== BANK MANAGEMENT SYSTEM ====");
            System.out.println("1. Create Account");
            System.out.println("2. Check Balance");
            System.out.println("3. Deposit");
            System.out.println("4. Withdraw");
            System.out.println("5. View Account Details");
            System.out.println("0. Exit");

            System.out.print("Enter choice: ");
            int choice = Integer.parseInt(sc.nextLine());

            switch (choice) {

                case 1:
                    bank.createAccount();
                    break;

                case 2:
                    bank.checkBalance();
                    break;

                case 3:
                    bank.deposit();
                    break;

                case 4:
                    bank.withdraw();
                    break;

                case 5:
                    bank.viewAccountDetails();
                    break;

                case 0:
                    System.out.println("Thank you for using Bank System!");
                    System.exit(0);

                default:
                    System.out.println("Invalid choice");
            }
        }
    }
}