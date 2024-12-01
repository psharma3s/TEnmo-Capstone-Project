package com.techelevator.tenmo.dao;

import java.math.BigDecimal;

public interface AccountDao {
    BigDecimal getBalance(int userId);
    BigDecimal getBalanceByAccountId(int accountId);

    int findAccountByUserId(int userId);
}
