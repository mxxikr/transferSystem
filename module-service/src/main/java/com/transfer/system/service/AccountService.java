package com.transfer.system.service;

import com.transfer.system.dto.AccountBalanceResponseDTO;
import com.transfer.system.dto.AccountCreateRequestDTO;
import com.transfer.system.dto.AccountResponseDTO;

import java.math.BigDecimal;
import java.util.UUID;

public interface AccountService {

    AccountResponseDTO createAccount(AccountCreateRequestDTO accountCreateRequestDTO);

    AccountResponseDTO getAccount(UUID id);

    void deleteAccount(UUID id);

    AccountBalanceResponseDTO deposit(String accountNumber, BigDecimal amount);

    AccountBalanceResponseDTO withdraw(String accountNumber, BigDecimal amount);
}