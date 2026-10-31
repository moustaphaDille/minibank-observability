package org.moustapha.transactionservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferRequest(
        @NotNull Long sourceAccountId,
        @NotNull Long targetAccountId,
        @NotNull @DecimalMin("0.01") BigDecimal amount) {}
