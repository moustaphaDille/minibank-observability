package org.moustapha.transactionservice.dto;

import java.math.BigDecimal;

public record AccountDto(Long id, Long customerId, BigDecimal balance) {}
