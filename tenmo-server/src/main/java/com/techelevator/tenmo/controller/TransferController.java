package com.techelevator.tenmo.controller;

import com.techelevator.tenmo.dao.AccountDao;
import com.techelevator.tenmo.dao.TransferDao;
import com.techelevator.tenmo.model.Transfer;
import com.techelevator.tenmo.model.TransferDto;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/transfer")
@PreAuthorize("isAuthenticated()")
public class TransferController {

    private final TransferDao transferDao;
    private final AccountDao accountDao;


    public TransferController(TransferDao transferDao, AccountDao accountDao) {
        this.transferDao = transferDao;
        this.accountDao = accountDao;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Transfer createTransfer(@RequestBody TransferDto transferDto) {

        // Ensure this is only for "Send" transfers
        if (transferDto.getTransferTypeId() != 2) { // 2 = Send
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only 'Send' transfers are allowed here.");
        }

        // Validate amount and check balance
        if (transferDto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transfer amount must be positive.");
        }
        BigDecimal senderBalance = accountDao.getBalance(transferDto.getFromUserId());
        if (senderBalance.compareTo(transferDto.getAmount()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient funds for the transfer.");
        }

        // Create the transfer with Approved status
        int accountFromId = accountDao.findAccountByUserId(transferDto.getFromUserId());
        int accountToId = accountDao.findAccountByUserId(transferDto.getToUserId());

        Transfer transfer = new Transfer();
        transfer.setTransferTypeId(2); // Send
        transfer.setTransferStatusId(2); // Approved immediately
        transfer.setAccountFrom(accountFromId);
        transfer.setAccountTo(accountToId);
        transfer.setAmount(transferDto.getAmount());

        // Update balances
        Transfer createdTransfer = transferDao.createTransfer(transfer);
        transferDao.updateAccountBalance(accountFromId, transferDto.getAmount().negate());
        transferDao.updateAccountBalance(accountToId, transferDto.getAmount());

        return createdTransfer;
    }



    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<Transfer> getTransfers(@RequestParam int userId) {
        return transferDao.getTransfersByUserId(userId);
    }

    @GetMapping("/{transferId}")
    @PreAuthorize("isAuthenticated()")
    public Transfer getTransferById(@PathVariable int transferId) {
        return transferDao.getTransferById(transferId);
    }

    @PostMapping("/request")
    @ResponseStatus(HttpStatus.CREATED)
    public Transfer requestTransfer(@RequestBody TransferDto transferDto) {
        if (transferDto.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Transfer amount must be positive.");
        }
        if (transferDto.getFromUserId() == transferDto.getToUserId()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot request money from yourself.");
        }

        Transfer transfer = new Transfer();
        transfer.setTransferTypeId(1); // Request type
        transfer.setTransferStatusId(1); // Pending status
        transfer.setAccountFrom(accountDao.findAccountByUserId(transferDto.getToUserId())); // Approver's account
        transfer.setAccountTo(accountDao.findAccountByUserId(transferDto.getFromUserId())); // Requester's account
        transfer.setAmount(transferDto.getAmount());

        return transferDao.createTransfer(transfer);
    }






    @GetMapping("/pending")
    public List<Transfer> getPendingTransfers(@RequestParam int userId) {
        return transferDao.getPendingTransfersByUserId(userId);
    }




    @PutMapping("/{transferId}/approve")
    public void approveTransfer(@PathVariable int transferId) {
        Transfer transfer = transferDao.getTransferById(transferId);

        // Ensure it’s a Request transfer and currently Pending
        if (transfer.getTransferTypeId() != 1 || transfer.getTransferStatusId() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending 'Request' transfers can be approved.");
        }

        // Check if the approver has enough balance
        BigDecimal approverBalance = accountDao.getBalance(transfer.getAccountFrom());
        if (approverBalance.compareTo(transfer.getAmount()) < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient funds to approve this request.");
        }

        // Update the transfer status to Approved
        transferDao.approveOrRejectTransfer(transferId, true);

        // Update balances: Deduct from approver and add to requester
        transferDao.updateAccountBalance(transfer.getAccountFrom(), transfer.getAmount().negate());
        transferDao.updateAccountBalance(transfer.getAccountTo(), transfer.getAmount());
    }

    @PutMapping("/{transferId}/reject")
    public void rejectTransfer(@PathVariable int transferId) {
        Transfer transfer = transferDao.getTransferById(transferId);

        // Ensure it’s a Request transfer and currently Pending
        if (transfer.getTransferTypeId() != 1 || transfer.getTransferStatusId() != 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only pending 'Request' transfers can be rejected.");
        }

        // Update the transfer status to Rejected
        transferDao.approveOrRejectTransfer(transferId, false);
    }





}
