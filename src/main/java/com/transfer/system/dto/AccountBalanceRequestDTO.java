package com.transfer.system.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
public class AccountBalanceRequestDTO {
    private String accountNumber;
    private BigDecimal amount;
}