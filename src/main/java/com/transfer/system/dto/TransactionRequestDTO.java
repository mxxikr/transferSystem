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
public class TransactionRequestDTO {
    private String fromAccountNumber; // 송금 계좌 번호
    private String toAccountNumber; // 수신 계좌 번호
    private BigDecimal amount; // 송금 금액
}