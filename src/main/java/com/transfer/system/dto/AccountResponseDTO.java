package com.transfer.system.dto;

import com.transfer.system.enums.AccountStatus;
import com.transfer.system.enums.AccountType;
import com.transfer.system.enums.CurrencyType;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class AccountResponseDTO {
    private Long accountId;
    private String accountNumber;
    private String accountName;
    private String bankName;
    private AccountType accountType;
    private CurrencyType currencyType;
    private BigDecimal balance;
    private AccountStatus accountStatus;
    private LocalDateTime createdTimeStamp;
    private LocalDateTime updatedTimeStamp;
}
