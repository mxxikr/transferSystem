package com.transfer.system.dto;


import com.transfer.system.enums.AccountType;
import com.transfer.system.enums.CurrencyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountCreateRequestDTO {
    private String accountName; // 계좌 이름
    private AccountType accountType; // 계좌 유형
    private CurrencyType currencyType; // 통화 유형
}