import com.sun.net.httpserver.*;
import java.io.*;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.stream.Collectors;

public class BankServer {

    static Connection con;

    public static void main(String[] args) throws Exception {

        // DB CONNECTION
        con = DriverManager.getConnection(
                "jdbc:mysql://localhost:3306/bankdb",
                "root",
                ""
        );

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);

        server.createContext("/createAccount", BankServer::createAccount);
        server.createContext("/deposit", BankServer::deposit);
        server.createContext("/withdraw", BankServer::withdraw);
        server.createContext("/balance", BankServer::balance);
        server.createContext("/transfer", BankServer::transfer);

        server.setExecutor(null);
        server.start();

        System.out.println("🚀 Server running at http://localhost:8080");
    }

    // ✅ COMMON RESPONSE METHOD
    static void sendResponse(HttpExchange exchange, String response) throws IOException {
        exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(200, response.getBytes().length);
        OutputStream os = exchange.getResponseBody();
        os.write(response.getBytes());
        os.close();
    }

    // ================= CREATE ACCOUNT =================
    static void createAccount(HttpExchange exchange) throws IOException {
        try {
            String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))
                    .lines().collect(Collectors.joining());

            String name = body.split(":")[1].replace("\"", "").replace("}", "");

            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO accounts(name, balance) VALUES (?, 0)",
                    Statement.RETURN_GENERATED_KEYS
            );
            ps.setString(1, name);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            rs.next();
            int accNo = rs.getInt(1);

            String response =
                    "Account created successfully!\n" +
                    "Account Number: " + accNo + "\n" +
                    "Name: " + name + "\n" +
                    "Initial Balance: ₹0";

            System.out.println(response);
            sendResponse(exchange, response);

        } catch (Exception e) {
            sendResponse(exchange, "Error creating account");
        }
    }

    // ================= DEPOSIT =================
    static void deposit(HttpExchange exchange) throws IOException {
        try {
            String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))
                    .lines().collect(Collectors.joining());

            String[] parts = body.replace("{", "").replace("}", "")
                    .replace("\"", "").split(",");

            int accNo = Integer.parseInt(parts[0].split(":")[1]);
            double amount = Double.parseDouble(parts[1].split(":")[1]);

            PreparedStatement ps = con.prepareStatement(
                    "UPDATE accounts SET balance = balance + ? WHERE account_number = ?"
            );
            ps.setDouble(1, amount);
            ps.setInt(2, accNo);
            ps.executeUpdate();

            PreparedStatement ps2 = con.prepareStatement(
                    "SELECT balance FROM accounts WHERE account_number = ?"
            );
            ps2.setInt(1, accNo);
            ResultSet rs = ps2.executeQuery();
            rs.next();
            double newBalance = rs.getDouble("balance");

            String response =
                    "Deposit successful!\n" +
                    "Account Number: " + accNo + "\n" +
                    "Deposited Amount: ₹" + amount + "\n" +
                    "Updated Balance: ₹" + newBalance;

            System.out.println(response);
            sendResponse(exchange, response);

        } catch (Exception e) {
            sendResponse(exchange, "Error in deposit");
        }
    }

    // ================= WITHDRAW =================
    static void withdraw(HttpExchange exchange) throws IOException {
        try {
            String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))
                    .lines().collect(Collectors.joining());

            String[] parts = body.replace("{", "").replace("}", "")
                    .replace("\"", "").split(",");

            int accNo = Integer.parseInt(parts[0].split(":")[1]);
            double amount = Double.parseDouble(parts[1].split(":")[1]);

            PreparedStatement check = con.prepareStatement(
                    "SELECT balance FROM accounts WHERE account_number = ?"
            );
            check.setInt(1, accNo);
            ResultSet rs = check.executeQuery();
            rs.next();
            double balance = rs.getDouble("balance");

            if (balance < amount) {
                sendResponse(exchange, "Insufficient balance!");
                return;
            }

            PreparedStatement ps = con.prepareStatement(
                    "UPDATE accounts SET balance = balance - ? WHERE account_number = ?"
            );
            ps.setDouble(1, amount);
            ps.setInt(2, accNo);
            ps.executeUpdate();

            double newBalance = balance - amount;

            String response =
                    "Withdrawal successful!\n" +
                    "Account Number: " + accNo + "\n" +
                    "Withdrawn Amount: ₹" + amount + "\n" +
                    "Remaining Balance: ₹" + newBalance;

            System.out.println(response);
            sendResponse(exchange, response);

        } catch (Exception e) {
            sendResponse(exchange, "Error in withdrawal");
        }
    }

    // ================= CHECK BALANCE =================
    static void balance(HttpExchange exchange) throws IOException {
        try {
            String path = exchange.getRequestURI().getPath();
            int accNo = Integer.parseInt(path.substring(path.lastIndexOf("/") + 1));

            PreparedStatement ps = con.prepareStatement(
                    "SELECT balance FROM accounts WHERE account_number = ?"
            );
            ps.setInt(1, accNo);
            ResultSet rs = ps.executeQuery();
            rs.next();

            double balance = rs.getDouble("balance");

            String response =
                    "Account Number: " + accNo + "\n" +
                    "Current Balance: ₹" + balance;

            System.out.println(response);
            sendResponse(exchange, response);

        } catch (Exception e) {
            sendResponse(exchange, "Error fetching balance");
        }
    }

    // ================= TRANSFER =================
    static void transfer(HttpExchange exchange) throws IOException {
        try {
            String body = new BufferedReader(new InputStreamReader(exchange.getRequestBody()))
                    .lines().collect(Collectors.joining());

            String[] parts = body.replace("{", "").replace("}", "")
                    .replace("\"", "").split(",");

            int fromAcc = Integer.parseInt(parts[0].split(":")[1]);
            int toAcc = Integer.parseInt(parts[1].split(":")[1]);
            double amount = Double.parseDouble(parts[2].split(":")[1]);

            con.setAutoCommit(false);

            PreparedStatement check = con.prepareStatement(
                    "SELECT balance FROM accounts WHERE account_number = ?"
            );
            check.setInt(1, fromAcc);
            ResultSet rs = check.executeQuery();
            rs.next();
            double balance = rs.getDouble("balance");

            if (balance < amount) {
                sendResponse(exchange, "Insufficient balance for transfer!");
                return;
            }

            PreparedStatement debit = con.prepareStatement(
                    "UPDATE accounts SET balance = balance - ? WHERE account_number = ?"
            );
            debit.setDouble(1, amount);
            debit.setInt(2, fromAcc);
            debit.executeUpdate();

            PreparedStatement credit = con.prepareStatement(
                    "UPDATE accounts SET balance = balance + ? WHERE account_number = ?"
            );
            credit.setDouble(1, amount);
            credit.setInt(2, toAcc);
            credit.executeUpdate();

            con.commit();

            String response =
                    "Transfer successful!\n" +
                    "From Account: " + fromAcc + "\n" +
                    "To Account: " + toAcc + "\n" +
                    "Transferred Amount: ₹" + amount;

            System.out.println(response);
            sendResponse(exchange, response);

        } catch (Exception e) {
            try { con.rollback(); } catch (Exception ignored) {}
            sendResponse(exchange, "Error in transfer");
        }
    }
}