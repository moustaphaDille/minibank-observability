package org.moustapha.accountservice.dto;

import org.moustapha.accountservice.model.Account;

import java.math.BigDecimal;

public record AccountResponse(Long id, Long customerId, BigDecimal balance) {

    public static AccountResponse from(Account a) {
        return new AccountResponse(a.getId(), a.getCustomerId(), a.getBalance());
    }
}
