package com.transfer.system.service;

import com.transfer.system.domain.AccountEntity;
import com.transfer.system.domain.TransactionEntity;
import com.transfer.system.dto.AccountCreateRequestDTO;
import com.transfer.system.dto.AccountResponseDTO;
import com.transfer.system.enums.AccountStatus;
import com.transfer.system.exception.ErrorCode;
import com.transfer.system.exception.TransferSystemException;
import com.transfer.system.policy.TransferPolicy;
import com.transfer.system.repository.AccountRepository;
import com.transfer.system.repository.TransactionRepository;
import com.transfer.system.enums.TransactionType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final TransferPolicy transferPolicy;
    private final TransactionRepository transactionRepository;

    /***
     * 계좌 생성
     */
    @Override
    @Transactional
    public AccountResponseDTO createAccount(AccountCreateRequestDTO accountCreateRequestDTO) {
        if (accountCreateRequestDTO == null) {
            throw new TransferSystemException(ErrorCode.INVALID_REQUEST);
        }

        if (accountCreateRequestDTO.getAccountNumber() == null || accountCreateRequestDTO.getAccountName() == null ||
            accountCreateRequestDTO.getBankName() == null || accountCreateRequestDTO.getAccountType() == null ||
            accountCreateRequestDTO.getCurrencyType() == null || accountCreateRequestDTO.getBalance() == null) {
            throw new TransferSystemException(ErrorCode.INVALID_REQUEST);
        }

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
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void deposit(String accountNumber, BigDecimal amount) {
        if (accountNumber == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransferSystemException(ErrorCode.INVALID_REQUEST);
        }

        AccountEntity accountEntity = accountRepository.findByAccountNumberLock(accountNumber)
            .orElseThrow(() -> new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));
        accountEntity.addBalance(amount);
        accountRepository.save(accountEntity);

        // 입금 거래 기록 저장
        TransactionEntity transactionEntity = TransactionEntity.builder()
            .fromAccount(null)
            .toAccount(accountEntity)
            .transactionType(TransactionType.DEPOSIT)
            .amount(amount)
            .fee(BigDecimal.ZERO)
            .createdTimeStamp(LocalDateTime.now())
            .build();

        transactionRepository.save(transactionEntity);
    }

    /**
     * 계좌 출금
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void withdraw(String accountNumber, BigDecimal amount) {
        if (accountNumber == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransferSystemException(ErrorCode.INVALID_REQUEST);
        }

        AccountEntity accountEntity = accountRepository.findByAccountNumberLock(accountNumber)
            .orElseThrow(() -> new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));

        BigDecimal todayTotal = transactionRepository.getTodayWithdrawTotalFromAccount(accountNumber);
        todayTotal = todayTotal != null ? todayTotal : BigDecimal.ZERO;

        transferPolicy.validateWithdrawAmount(accountNumber, amount, todayTotal);

        if (accountEntity.getBalance().compareTo(amount) < 0) {
            throw new TransferSystemException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        accountEntity.subtractBalance(amount);
        accountRepository.save(accountEntity);

        // 출금 거래 기록 저장
        TransactionEntity transactionEntity = TransactionEntity.builder()
            .fromAccount(accountEntity)
            .toAccount(null)
            .transactionType(TransactionType.WITHDRAW)
            .amount(amount)
            .fee(BigDecimal.ZERO)
            .createdTimeStamp(LocalDateTime.now())
            .build();

        transactionRepository.save(transactionEntity);
    }
}