package com.transfer.system.service;

import com.transfer.system.dto.AccountRequestDTO;
import com.transfer.system.dto.AccountResponseDTO;

import java.math.BigDecimal;

public interface AccountService {

    AccountResponseDTO createAccount(AccountRequestDTO dto);

    AccountResponseDTO getAccount(Long id);

    void deleteAccount(Long id);

    void deposit(String accountNumber, BigDecimal amount);

    void withdraw(String accountNumber, BigDecimal amount);
}