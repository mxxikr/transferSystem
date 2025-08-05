package com.transfer.system.dto;

import com.transfer.system.domain.TransactionEntity;
import com.transfer.system.enums.TransactionType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class TransactionResponseDTO {
    private UUID transactionId;
    private String fromAccountNumber;
    private String toAccountNumber;
    private TransactionType transactionType;
    private BigDecimal amount;
    private BigDecimal fee;
    private LocalDateTime createdTimeStamp;

    public static TransactionResponseDTO from(TransactionEntity entity) {
        return TransactionResponseDTO.builder()
            .transactionId(entity.getTransactionId())
            .fromAccountNumber(entity.getFromAccount().getAccountNumber())
            .toAccountNumber(entity.getToAccount().getAccountNumber())
            .transactionType(entity.getTransactionType())
            .amount(entity.getAmount())
            .fee(entity.getFee())
            .createdTimeStamp(entity.getCreatedTimeStamp())
            .build();
    }
}