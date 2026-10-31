package org.moustapha.transactionservice.dto;

import org.moustapha.transactionservice.model.Transaction;
import org.moustapha.transactionservice.model.TransactionStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record TransactionResponse(Long id, Long sourceAccountId, Long targetAccountId,
                                  BigDecimal amount, TransactionStatus status,
                                  Instant createdAt) {

    public static TransactionResponse from(Transaction t) {
        return new TransactionResponse(t.getId(), t.getSourceAccountId(),
                t.getTargetAccountId(), t.getAmount(), t.getStatus(), t.getCreatedAt());
    }
}
