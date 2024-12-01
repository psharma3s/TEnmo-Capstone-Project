package com.techelevator.tenmo.dao;

import com.techelevator.tenmo.model.Transfer;
import java.math.BigDecimal;
import java.util.List;

public interface TransferDao {

    Transfer createTransfer(Transfer transfer);

    void updateAccountBalance(int accountId, BigDecimal amount);

    List<Transfer> getTransfersByUserId(int userId);

    Transfer getTransferById(int transferId);

    List<Transfer> getPendingTransfersByUserId(int userId);
    void approveOrRejectTransfer(int transferId, boolean approve);


}
