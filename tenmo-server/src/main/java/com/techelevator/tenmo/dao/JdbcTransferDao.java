package com.techelevator.tenmo.dao;

import com.techelevator.tenmo.exception.DaoException;
import com.techelevator.tenmo.model.Transfer;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class JdbcTransferDao implements TransferDao {

    private final JdbcTemplate jdbcTemplate;
    private final AccountDao accountDao;

    public JdbcTransferDao(JdbcTemplate jdbcTemplate, AccountDao accountDao) {
        this.jdbcTemplate = jdbcTemplate;
        this.accountDao = accountDao;
    }

    @Override
    public Transfer createTransfer(Transfer transfer) {
        String sql = "INSERT INTO transfer (transfer_type_id, transfer_status_id, account_from, account_to, amount) " +
                "VALUES (?, ?, ?, ?, ?) RETURNING transfer_id";

        try {
            int newTransferId = jdbcTemplate.queryForObject(sql, int.class,
                    transfer.getTransferTypeId(),
                    transfer.getTransferStatusId(),
                    transfer.getAccountFrom(),
                    transfer.getAccountTo(),
                    transfer.getAmount());

            transfer.setTransferId(newTransferId);
            return transfer;
        } catch (CannotGetJdbcConnectionException e) {
            throw new DaoException("Unable to connect to server or database", e);
        }
    }

    @Override
    public void updateAccountBalance(int accountId, BigDecimal amount) {
        String sql = "UPDATE account SET balance = balance + ? WHERE account_id = ?";
        try {
            jdbcTemplate.update(sql, amount, accountId);
        } catch (Exception e) {
            throw new DaoException("Error updating account balance for account ID: " + accountId, e);
        }
    }

    @Override
    public List<Transfer> getTransfersByUserId(int userId) {
        List<Transfer> transfers = new ArrayList<>();
        String sql = "SELECT t.transfer_id, t.transfer_type_id, t.transfer_status_id, " +
                "t.account_from, t.account_to, t.amount, " +
                "uf.username AS from_user, ut.username AS to_user " +
                "FROM transfer t " +
                "JOIN account af ON t.account_from = af.account_id " +
                "JOIN account at ON t.account_to = at.account_id " +
                "JOIN tenmo_user uf ON af.user_id = uf.user_id " +
                "JOIN tenmo_user ut ON at.user_id = ut.user_id " +
                "WHERE af.user_id = ? OR at.user_id = ?";

        SqlRowSet results = jdbcTemplate.queryForRowSet(sql, userId, userId);
        while (results.next()) {
            Transfer transfer = mapRowToTransfer(results);
            transfer.setFromUser(results.getString("from_user"));
            transfer.setToUser(results.getString("to_user"));
            transfers.add(transfer);
        }
        return transfers;
    }

    @Override
    public Transfer getTransferById(int transferId) {
        String sql = "SELECT t.transfer_id, t.transfer_type_id, t.transfer_status_id, " +
                "t.account_from, t.account_to, t.amount, " +
                "uf.username AS from_user, ut.username AS to_user " +
                "FROM transfer t " +
                "JOIN account af ON t.account_from = af.account_id " +
                "JOIN account at ON t.account_to = at.account_id " +
                "JOIN tenmo_user uf ON af.user_id = uf.user_id " +
                "JOIN tenmo_user ut ON at.user_id = ut.user_id " +
                "WHERE t.transfer_id = ?";

        SqlRowSet results = jdbcTemplate.queryForRowSet(sql, transferId);
        if (results.next()) {
            Transfer transfer = mapRowToTransfer(results);
            transfer.setFromUser(results.getString("from_user"));
            transfer.setToUser(results.getString("to_user"));
            return transfer;
        }
        return null;
    }

    @Override
    public List<Transfer> getPendingTransfersByUserId(int userId) {
        String sql = "SELECT t.transfer_id, t.transfer_type_id, t.transfer_status_id, " +
                "t.account_from, t.account_to, t.amount, " +
                "uf.username AS from_user, ut.username AS to_user " +
                "FROM transfer t " +
                "JOIN account af ON t.account_from = af.account_id " +
                "JOIN account at ON t.account_to = at.account_id " +
                "JOIN tenmo_user uf ON af.user_id = uf.user_id " +
                "JOIN tenmo_user ut ON at.user_id = ut.user_id " +
                "WHERE t.transfer_status_id = 1 AND af.user_id = ?";

        List<Transfer> pendingTransfers = new ArrayList<>();
        SqlRowSet results = jdbcTemplate.queryForRowSet(sql, userId);
        while (results.next()) {
            Transfer transfer = mapRowToTransfer(results);
            transfer.setFromUser(results.getString("from_user"));
            transfer.setToUser(results.getString("to_user"));
            pendingTransfers.add(transfer);
        }

        return pendingTransfers;
    }













    @Override
    public void approveOrRejectTransfer(int transferId, boolean approve) {
        String getTransferSql = "SELECT account_from, account_to, amount FROM transfer WHERE transfer_id = ?";
        SqlRowSet results = jdbcTemplate.queryForRowSet(getTransferSql, transferId);

        if (results.next()) {
            int accountFrom = results.getInt("account_from"); // Approver's account
            int accountTo = results.getInt("account_to");     // Requester's account
            BigDecimal amount = results.getBigDecimal("amount");

            if (approve) {
                // Step 1: Check if approver has sufficient funds
                BigDecimal approverBalance = accountDao.getBalance(accountFrom);
                if (approverBalance.compareTo(amount) < 0) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Insufficient funds to approve transfer.");
                }

                // Step 2: Update the transfer status to Approved
                String updateStatusSql = "UPDATE transfer SET transfer_status_id = 2 WHERE transfer_id = ?";
                jdbcTemplate.update(updateStatusSql, transferId);

                // Step 3: Debit the approver's account and credit the requestor's account
                updateAccountBalance(accountFrom, amount.negate()); // Subtract from approver's balance
                updateAccountBalance(accountTo, amount);            // Add to requestor's balance

            } else {
                // Reject the transfer and update the status
                String updateStatusSql = "UPDATE transfer SET transfer_status_id = 3 WHERE transfer_id = ?";
                jdbcTemplate.update(updateStatusSql, transferId);
            }
        }
    }


    private BigDecimal getAccountBalance(int accountId) {
        String sql = "SELECT balance FROM account WHERE account_id = ?";
        SqlRowSet result = jdbcTemplate.queryForRowSet(sql, accountId);
        if (result.next()) {
            return result.getBigDecimal("balance");
        }
        return BigDecimal.ZERO;
    }

    private void updateTransferStatus(int transferId, int statusId) {
        String sql = "UPDATE transfer SET transfer_status_id = ? WHERE transfer_id = ?";
        jdbcTemplate.update(sql, statusId, transferId);
    }

    private Transfer mapRowToTransfer(SqlRowSet rs) {
        Transfer transfer = new Transfer();
        transfer.setTransferId(rs.getInt("transfer_id"));
        transfer.setTransferTypeId(rs.getInt("transfer_type_id"));
        transfer.setTransferStatusId(rs.getInt("transfer_status_id"));
        transfer.setAccountFrom(rs.getInt("account_from"));
        transfer.setAccountTo(rs.getInt("account_to"));
        transfer.setAmount(rs.getBigDecimal("amount"));
        return transfer;
    }
}
