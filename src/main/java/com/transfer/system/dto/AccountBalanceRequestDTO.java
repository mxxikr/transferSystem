package com.transfer.system.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountBalanceRequestDTO {
    private String accountNumber; // 계좌 번호
    private BigDecimal amount; // 잔액
}