package com.transfer.system.dto;

import com.transfer.system.domain.AccountEntity;
import com.transfer.system.enums.AccountStatus;
import com.transfer.system.enums.AccountType;
import com.transfer.system.enums.CurrencyType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Builder
public class AccountResponseDTO {
    private UUID accountId;
    private String accountNumber;
    private String accountName;
    private String bankName;
    private AccountType accountType;
    private CurrencyType currencyType;
    private BigDecimal balance;
    private AccountStatus accountStatus;
    private LocalDateTime createdTimeStamp;
    private LocalDateTime updatedTimeStamp;

    public static AccountResponseDTO from(AccountEntity entity) {
        return AccountResponseDTO.builder()
            .accountId(entity.getAccountId())
            .accountNumber(entity.getAccountNumber())
            .accountName(entity.getAccountName())
            .bankName(entity.getBankName())
            .accountType(entity.getAccountType())
            .currencyType(entity.getCurrencyType())
            .balance(entity.getBalance() != null ? entity.getBalance() : BigDecimal.ZERO)
            .accountStatus(entity.getAccountStatus())
            .createdTimeStamp(entity.getCreatedTimeStamp())
            .updatedTimeStamp(entity.getUpdatedTimeStamp())
            .build();
    }
}