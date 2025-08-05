package com.transfer.system.service;

import com.transfer.system.domain.AccountEntity;
import com.transfer.system.dto.AccountCreateRequestDTO;
import com.transfer.system.dto.AccountResponseDTO;
import com.transfer.system.enums.AccountStatus;
import com.transfer.system.exception.ErrorCode;
import com.transfer.system.exception.TransferSystemException;
import com.transfer.system.policy.TransferPolicy;
import com.transfer.system.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final TransferPolicy transferPolicy;

    /***
     * 계좌 생성
     */
    @Override
    public AccountResponseDTO createAccount(AccountCreateRequestDTO accountCreateRequestDTO) {
        if (accountRepository.existsByAccountNumber(accountCreateRequestDTO.getAccountNumber())) {
            throw new TransferSystemException(ErrorCode.DUPLICATE_ACCOUNT_NUMBER);
        }

        AccountEntity accountEntity = AccountEntity.builder()
            .accountNumber(accountCreateRequestDTO.getAccountNumber())
            .accountName(accountCreateRequestDTO.getAccountName())
            .bankName(accountCreateRequestDTO.getBankName())
            .accountType(accountCreateRequestDTO.getAccountType())
            .currencyType(accountCreateRequestDTO.getCurrencyType())
            .balance(accountCreateRequestDTO.getBalance())
            .accountStatus(accountCreateRequestDTO.getAccountStatus() != null ? accountCreateRequestDTO.getAccountStatus() : AccountStatus.ACTIVE)
            .createdTimeStamp(LocalDateTime.now())
            .updatedTimeStamp(LocalDateTime.now())
            .build();

        return AccountResponseDTO.from(accountRepository.save(accountEntity));
    }

    /**
     * 계좌 조회
     */
    @Override
    public AccountResponseDTO getAccount(UUID id) {
        AccountEntity accountEntity = accountRepository.findById(id)
                .orElseThrow(() -> new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));
        return AccountResponseDTO.from(accountEntity);
    }

    /**
     * 계좌 삭제
     */
    @Override
    public void deleteAccount(UUID id) {
        AccountEntity accountEntity = accountRepository.findById(id)
                .orElseThrow(() -> new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));
        accountRepository.delete(accountEntity);
    }

    /**
     * 계좌 입금
     */
    @Override
    public void deposit(String accountNumber, BigDecimal amount) {
        AccountEntity accountEntity = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));
        accountEntity.addBalance(amount);
        accountRepository.save(accountEntity);
    }

    /**
     * 계좌 출금
     */
    @Override
    public void withdraw(String accountNumber, BigDecimal amount) {
        AccountEntity accountEntity = accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));
        accountEntity.subtractBalance(amount);
        accountRepository.save(accountEntity);
    }
}