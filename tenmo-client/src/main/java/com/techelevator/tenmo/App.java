package com.techelevator.tenmo;

import com.techelevator.tenmo.model.*;
import com.techelevator.tenmo.services.*;

import java.math.BigDecimal;
import java.util.List;

public class App {

    private static final String API_BASE_URL = "http://localhost:8080/";

    private final ConsoleService consoleService = new ConsoleService();
    private final AuthenticationService authenticationService = new AuthenticationService(API_BASE_URL);
    private final UserService userService = new UserService(API_BASE_URL);
    private final TransferService transferService = new TransferService(API_BASE_URL);


    private AuthenticatedUser currentUser;

    public static void main(String[] args) {
        App app = new App();
        app.run();
    }

    private void run() {
        consoleService.printGreeting();
        loginMenu();
        if (currentUser != null) {
            mainMenu();
        }
    }

    private void loginMenu() {
        int menuSelection = -1;
        while (menuSelection != 0 && currentUser == null) {
            consoleService.printLoginMenu();
            menuSelection = consoleService.promptForMenuSelection("Please choose an option: ");
            if (menuSelection == 1) {
                handleRegister();
            } else if (menuSelection == 2) {
                handleLogin();
            } else if (menuSelection != 0) {
                System.out.println("Invalid Selection");
                consoleService.pause();
            }
        }
    }

    private void handleRegister() {
        System.out.println("Please register a new user account");
        UserCredentials credentials = consoleService.promptForCredentials();
        if (authenticationService.register(credentials)) {
            System.out.println("Registration successful. You can now login.");
        } else {
            consoleService.printErrorMessage();
        }
    }

    private void handleLogin() {
        UserCredentials credentials = consoleService.promptForCredentials();
        currentUser = authenticationService.login(credentials);
        if (currentUser == null) {
            consoleService.printErrorMessage();
        }
    }

    private void mainMenu() {
        int menuSelection = -1;
        while (menuSelection != 0) {
            consoleService.printMainMenu();
            menuSelection = consoleService.promptForMenuSelection("Please choose an option: ");
            if (menuSelection == 1) {
                viewCurrentBalance();
            } else if (menuSelection == 2) {
                viewTransferHistory();
            } else if (menuSelection == 3) {
                viewPendingRequests();
            } else if (menuSelection == 4) {
                sendBucks();
            } else if (menuSelection == 5) {
                requestBucks();
            } else if (menuSelection == 0) {
                continue;
            } else {
                System.out.println("Invalid Selection");
            }
            consoleService.pause();
        }
    }

    private void viewCurrentBalance() {
        // TODO Auto-generated method stub
        if (currentUser == null) {
            System.out.println("Please log in to view your balance.");
            return;
        }

        AccountService accountService = new AccountService(API_BASE_URL);
        BigDecimal balance = accountService.getBalance(currentUser);
        if (balance != null) {
            System.out.println("Your current balance is: " + balance);
        } else {
            System.out.println("Unable to retrieve balance.");
        }
    }

    private void displayUserList() {
        UserService userService = new UserService(API_BASE_URL);
        List<User> users = userService.listUsers(currentUser);
        System.out.println("ID   Username");
        for (User user : users) {
            System.out.println(user.getId() + "   " + user.getUsername());
        }
    }


    private void viewTransferHistory() {
        // TODO Auto-generated method stub
        if (currentUser == null) {
            System.out.println("Please log in to view your transfer history.");
            return;
        }

        TransferService transferService = new TransferService(API_BASE_URL);
        List<Transfer> transfers = transferService.getTransfers(currentUser);

        System.out.println("-------------------------------------------");
        System.out.println("Transfers");
        System.out.println("ID          From/To                 Amount");
        System.out.println("-------------------------------------------");

        for (Transfer transfer : transfers) {
            String fromOrTo = transfer.getAccountFrom() == currentUser.getUser().getId() ? "To: " + transfer.getToUser() : "From: " + transfer.getFromUser();
            System.out.printf("%-12d %-20s $ %10.2f%n", transfer.getTransferId(), fromOrTo, transfer.getAmount());
        }

        System.out.println("---------");
        int transferId = consoleService.promptForInt("Please enter transfer ID to view details (0 to cancel): ");
        if (transferId != 0) {
            viewTransferDetails(transferId);
        }
    }

    private void viewTransferDetails(int transferId) {
        if (currentUser == null) {
            System.out.println("You need to be logged in to view transfer details.");
            return;
        }

        TransferService transferService = new TransferService(API_BASE_URL);
        Transfer transfer = transferService.getTransferById(currentUser, transferId);

        if (transfer == null) {
            System.out.println("Transfer not found.");
            return ;
        } else {

            System.out.println("-------------------------------------------");
            System.out.println("Transfer Details");
            System.out.println("-------------------------------------------");
            System.out.println("ID: " + transfer.getTransferId());
            System.out.println("From: " + transfer.getFromUser());
            System.out.println("To: " + transfer.getToUser());
            System.out.println("Type: " + (transfer.getTransferTypeId() == 2 ? "Send" : "Request"));
            System.out.println("Status: " + (transfer.getTransferStatusId() == 2 ? "Approved" :
                    (transfer.getTransferStatusId() == 1 ? "Pending" : "Rejected")));
            System.out.printf("Amount: $ %.2f%n", transfer.getAmount());
        }
    }
    private void viewPendingRequests() {
        List<Transfer> pendingTransfers = transferService.getPendingTransfers(currentUser);
        System.out.println("Pending transfers count: " + pendingTransfers.size()); // Debug line

        if (pendingTransfers == null || pendingTransfers.isEmpty()) {
            System.out.println("You have no pending transfers!");
        } else {
            System.out.println("-------------------------------------------");
            System.out.println("Pending Transfers");
            System.out.println("ID          From                     Amount");
            System.out.println("-------------------------------------------");

            for (Transfer transfer : pendingTransfers) {
                System.out.printf("%-12d %-20s $ %10.2f%n", transfer.getTransferId(), transfer.getToUser(), transfer.getAmount());
            }

            System.out.println("---------");
            int transferId = consoleService.promptForInt("Please enter transfer ID to approve/reject (0 to cancel): ");
            if (transferId != 0) {
                approveOrRejectTransfer(transferId);
            }
        }
    }









    private void sendBucks() {

        UserService userService = new UserService(API_BASE_URL);
        List<User> users = userService.listUsers(currentUser);

        if (users == null || users.isEmpty()) {
            System.out.println("No users available to send TE Bucks to.");
            return;
        }

        System.out.println("-------------------------------------------");
        System.out.println("Users");
        System.out.println("ID          Name");
        System.out.println("-------------------------------------------");

        for (User user : users) {
            // Only display other users (not the current user)
            if (user.getId() != currentUser.getUser().getId()) {
                System.out.printf("%-12d %s%n", user.getId(), user.getUsername());
            }
        }

        System.out.println("---------");


        int userIdToSendTo = consoleService.promptForInt("Enter ID of user you are sending to (0 to cancel): ");
        if (userIdToSendTo == 0) {
            System.out.println("Transfer canceled.");
            return;
        }

        BigDecimal amount = consoleService.promptForBigDecimal("Enter amount: ");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("Transfer amount must be greater than zero.");
            return;
        }


        TransferDto transferDto = new TransferDto();
        transferDto.setFromUserId(currentUser.getUser().getId());
        transferDto.setToUserId(userIdToSendTo);
        transferDto.setAmount(amount);
        transferDto.setTransferTypeId(2); // 2 for "Send"


        boolean success = transferService.sendBucks(currentUser, transferDto);
        if (success) {
            System.out.println("Transfer successful.");
        } else {
            System.out.println("Transfer failed.");
        }
    }





    private void requestBucks() {
        // Step 1: List available users to request bucks from
        UserService userService = new UserService(API_BASE_URL);
        List<User> users = userService.listUsers(currentUser);

        if (users == null || users.isEmpty()) {
            System.out.println("No users available to request TE Bucks from.");
            return;
        }

        System.out.println("-------------------------------------------");
        System.out.println("Users");
        System.out.println("ID          Name");
        System.out.println("-------------------------------------------");

        for (User user : users) {
            // Only display other users (not the current user)
            if (user.getId() != currentUser.getUser().getId()) {
                System.out.printf("%-12d %s%n", user.getId(), user.getUsername());
            }
        }

        System.out.println("---------");

        // Step 2: Prompt user to select a requester and an amount
        int userIdToRequestFrom = consoleService.promptForInt("Enter ID of user you are requesting from (0 to cancel): ");
        if (userIdToRequestFrom == 0) {
            System.out.println("Request canceled.");
            return;
        }

        BigDecimal amount = consoleService.promptForBigDecimal("Enter amount: ");
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            System.out.println("Request amount must be greater than zero.");
            return;
        }


        TransferDto transferDto = new TransferDto();
        transferDto.setFromUserId(userIdToRequestFrom);
        transferDto.setToUserId(currentUser.getUser().getId());
        transferDto.setAmount(amount);
        transferDto.setTransferTypeId(1); // 1 for "Request"

        // Step 4: Call TransferService to initiate the request
        boolean success = transferService.requestTransfer(currentUser, transferDto);
        if (success) {
            System.out.println("Request sent successfully.");
        } else {
            System.out.println("Request failed.");
        }
    }


    private void approveOrRejectTransfer(int transferId) {
        System.out.println("1: Approve");
        System.out.println("2: Reject");
        System.out.println("0: Don't approve or reject");
        int choice = consoleService.promptForInt("Please choose an option: ");

        if (choice == 1) {
            if (transferService.approveTransfer(currentUser, transferId)) {
                System.out.println("Transfer approved successfully.");
            } else {
                System.out.println("Failed to approve transfer.");
            }
        } else if (choice == 2) {
            if (transferService.rejectTransfer(currentUser, transferId)) {
                System.out.println("Transfer rejected successfully.");
            } else {
                System.out.println("Failed to reject transfer.");
            }
        }
    }





}

