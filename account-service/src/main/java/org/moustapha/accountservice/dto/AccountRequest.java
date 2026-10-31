package org.moustapha.accountservice.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record AccountRequest(
        @NotNull Long customerId,
        @NotNull @DecimalMin("0.00") BigDecimal initialBalance) {}
