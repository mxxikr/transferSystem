package com.transfer.system.dto;


import com.transfer.system.enums.AccountStatus;
import com.transfer.system.enums.AccountType;
import com.transfer.system.enums.CurrencyType;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
public class AccountRequestDTO {
    private String accountNumber;
    private String accountName;
    private String bankName;
    private AccountType accountType;
    private CurrencyType currencyType;
    private BigDecimal balance;

    private AccountStatus accountStatus;
}