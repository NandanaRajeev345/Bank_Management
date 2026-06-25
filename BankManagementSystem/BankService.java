import java.sql.*;
import java.util.Scanner;

public class BankService {

    Scanner sc = new Scanner(System.in);

    // ✅ CREATE ACCOUNT
    public void createAccount() {

        try {
            Connection con = DBConnection.getConnection();

            System.out.print("Enter name: ");
            String name = sc.nextLine();

            System.out.print("Enter phone: ");
            String phone = sc.nextLine();

            System.out.print("Enter email: ");
            String email = sc.nextLine();

            System.out.print("Enter address: ");
            String address = sc.nextLine();

            String customerQuery = "INSERT INTO customers(name, phone, email, address) VALUES (?, ?, ?, ?)";
            PreparedStatement psCustomer = con.prepareStatement(customerQuery, Statement.RETURN_GENERATED_KEYS);

            psCustomer.setString(1, name);
            psCustomer.setString(2, phone);
            psCustomer.setString(3, email);
            psCustomer.setString(4, address);

            int rows = psCustomer.executeUpdate();

            if (rows == 0) {
                System.out.println("Customer insert failed!");
                return;
            }

            ResultSet rs = psCustomer.getGeneratedKeys();
            int customerId = 0;

            if (rs.next()) {
                customerId = rs.getInt(1);
            }

            System.out.print("Enter account type (Savings/Current): ");
            String accType = sc.nextLine();

            System.out.print("Enter initial balance: ");
            double balance = Double.parseDouble(sc.nextLine());

            System.out.print("Set 4-digit PIN: ");
            int pin = Integer.parseInt(sc.nextLine());

            String accountQuery = "INSERT INTO accounts(customer_id, account_type, balance, pin) VALUES (?, ?, ?, ?)";
            PreparedStatement psAccount = con.prepareStatement(accountQuery, Statement.RETURN_GENERATED_KEYS);

            psAccount.setInt(1, customerId);
            psAccount.setString(2, accType);
            psAccount.setDouble(3, balance);
            psAccount.setInt(4, pin);

            psAccount.executeUpdate();

            ResultSet rsAcc = psAccount.getGeneratedKeys();

            if (rsAcc.next()) {
                int accountNumber = rsAcc.getInt(1);
                System.out.println("Account created successfully!");
                System.out.println("Your Account Number is: " + accountNumber);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ✅ CHECK BALANCE
    public void checkBalance() {

        try {
            Connection con = DBConnection.getConnection();

            System.out.print("Enter account number: ");
            int accNo = Integer.parseInt(sc.nextLine());

            System.out.print("Enter PIN: ");
            int pin = Integer.parseInt(sc.nextLine());

            String query = "SELECT balance FROM accounts WHERE account_number=? AND pin=?";
            PreparedStatement ps = con.prepareStatement(query);

            ps.setInt(1, accNo);
            ps.setInt(2, pin);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                System.out.println("Your Balance is: " + rs.getDouble("balance"));
            } else {
                System.out.println("Invalid account number or PIN");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ✅ DEPOSIT
    public void deposit() {

        try {
            Connection con = DBConnection.getConnection();

            System.out.print("Enter account number: ");
            int accNo = Integer.parseInt(sc.nextLine());

            System.out.print("Enter amount to deposit: ");
            double amount = Double.parseDouble(sc.nextLine());

            String query = "UPDATE accounts SET balance = balance + ? WHERE account_number=?";
            PreparedStatement ps = con.prepareStatement(query);

            ps.setDouble(1, amount);
            ps.setInt(2, accNo);

            int rows = ps.executeUpdate();

            if (rows > 0) {
                System.out.println("Amount deposited successfully!");
            } else {
                System.out.println("Account not found");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ✅ WITHDRAW
    public void withdraw() {

        try {
            Connection con = DBConnection.getConnection();

            System.out.print("Enter account number: ");
            int accNo = Integer.parseInt(sc.nextLine());

            System.out.print("Enter PIN: ");
            int pin = Integer.parseInt(sc.nextLine());

            System.out.print("Enter amount to withdraw: ");
            double amount = Double.parseDouble(sc.nextLine());

            String checkQuery = "SELECT balance FROM accounts WHERE account_number=? AND pin=?";
            PreparedStatement psCheck = con.prepareStatement(checkQuery);

            psCheck.setInt(1, accNo);
            psCheck.setInt(2, pin);

            ResultSet rs = psCheck.executeQuery();

            if (rs.next()) {
                double balance = rs.getDouble("balance");

                if (balance >= amount) {

                    String updateQuery = "UPDATE accounts SET balance = balance - ? WHERE account_number=?";
                    PreparedStatement psUpdate = con.prepareStatement(updateQuery);

                    psUpdate.setDouble(1, amount);
                    psUpdate.setInt(2, accNo);

                    psUpdate.executeUpdate();

                    System.out.println("Withdrawal successful!");
                } else {
                    System.out.println("Insufficient balance");
                }

            } else {
                System.out.println("Invalid account number or PIN");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ✅ VIEW ACCOUNT DETAILS (JOIN)
    public void viewAccountDetails() {

        try {
            Connection con = DBConnection.getConnection();

            System.out.print("Enter account number: ");
            int accNo = Integer.parseInt(sc.nextLine());

            String query = "SELECT c.name, c.phone, a.account_number, a.account_type, a.balance " +
                    "FROM customers c JOIN accounts a ON c.customer_id = a.customer_id " +
                    "WHERE a.account_number=?";

            PreparedStatement ps = con.prepareStatement(query);
            ps.setInt(1, accNo);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                System.out.println("Name: " + rs.getString("name"));
                System.out.println("Phone: " + rs.getString("phone"));
                System.out.println("Account Number: " + rs.getInt("account_number"));
                System.out.println("Account Type: " + rs.getString("account_type"));
                System.out.println("Balance: " + rs.getDouble("balance"));
            } else {
                System.out.println("Account not found");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}