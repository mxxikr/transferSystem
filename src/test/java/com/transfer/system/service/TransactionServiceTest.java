package com.transfer.system.service;

import com.transfer.system.domain.AccountEntity;
import com.transfer.system.domain.TransactionEntity;
import com.transfer.system.dto.TransactionRequestDTO;
import com.transfer.system.enums.AccountStatus;
import com.transfer.system.enums.AccountType;
import com.transfer.system.enums.CurrencyType;
import com.transfer.system.enums.TransactionType;
import com.transfer.system.exception.TransferSystemException;
import com.transfer.system.policy.TransferPolicy;
import com.transfer.system.repository.AccountRepository;
import com.transfer.system.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionServiceTest {

    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private TransferPolicy transferPolicy;
    private TransactionServiceImpl transactionService;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        transferPolicy = mock(TransferPolicy.class);
        transactionService = new TransactionServiceImpl(accountRepository, transactionRepository, transferPolicy);
    }

    /**
     * 정상 이체 테스트
     */
    @Test
    void transfer_success() {
        // given
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .fromAccountNumber("account123")
                .toAccountNumber("account456")
                .amount(new BigDecimal("100"))
                .build();

        AccountEntity from = AccountEntity.builder()
                .accountNumber("account123")
                .balance(new BigDecimal("1000"))
                .build();

        AccountEntity to = AccountEntity.builder()
                .accountNumber("account456")
                .balance(new BigDecimal("500"))
                .build();

        when(accountRepository.findByAccountNumber("account123")).thenReturn(Optional.of(from));
        when(accountRepository.findByAccountNumber("account456")).thenReturn(Optional.of(to));
        when(transferPolicy.calculateFee(any())).thenReturn(new BigDecimal("10"));
        when(transferPolicy.getTransferDailyLimit()).thenReturn(new BigDecimal("100000"));

        // when / then
        assertDoesNotThrow(() -> transactionService.transfer(request));
        verify(transactionRepository).save(any(TransactionEntity.class));
    }

    /**
     * 동일 계좌 간 이체 시도 테스트
     */
    @Test
    void transfer_sameAccount_shouldThrow() {
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .fromAccountNumber("account123")
                .toAccountNumber("account123")
                .amount(new BigDecimal("100"))
                .build();

        assertThrows(TransferSystemException.class, () -> transactionService.transfer(request));
    }

    /**
     * 출금 계좌 없음
     */
    @Test
    void transfer_fromAccountNotFound_shouldThrow() {
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .fromAccountNumber("account123")
                .toAccountNumber("account456")
                .amount(new BigDecimal("100"))
                .build();

        when(accountRepository.findByAccountNumber("account123")).thenReturn(Optional.empty());

        assertThrows(TransferSystemException.class, () -> transactionService.transfer(request));
    }

    /**
     * 입금 계좌 없음
     */
    @Test
    void transfer_toAccountNotFound_shouldThrow() {
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .fromAccountNumber("account123")
                .toAccountNumber("account456")
                .amount(new BigDecimal("100"))
                .build();

        when(accountRepository.findByAccountNumber("account123")).thenReturn(Optional.of(AccountEntity.builder()
                .accountNumber("account123")
                .balance(BigDecimal.valueOf(5000))
                .build()));
        when(accountRepository.findByAccountNumber("account456")).thenReturn(Optional.empty());

        assertThrows(TransferSystemException.class, () -> transactionService.transfer(request));
    }

    /**
     * 이체 한도 초과
     */
    @Test
    void transfer_limitExceeded_shouldThrow() {
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .fromAccountNumber("account123")
                .toAccountNumber("account456")
                .amount(new BigDecimal("1000000"))
                .build();

        AccountEntity from = AccountEntity.builder()
                .accountNumber("account123")
                .balance(BigDecimal.valueOf(2000000))
                .build();

        AccountEntity to = AccountEntity.builder()
                .accountNumber("account456")
                .balance(BigDecimal.ZERO)
                .build();

        when(accountRepository.findByAccountNumber("account123")).thenReturn(Optional.of(from));
        when(accountRepository.findByAccountNumber("account456")).thenReturn(Optional.of(to));
        when(transferPolicy.calculateFee(any())).thenReturn(new BigDecimal("10000"));
        when(transferPolicy.getTransferDailyLimit()).thenReturn(new BigDecimal("500000"));

        assertThrows(TransferSystemException.class, () -> transactionService.transfer(request));
    }

    /**
     * 출금 계좌 잔액 부족
     */
    @Test
    void transfer_insufficientBalance_shouldThrow() {
        TransactionRequestDTO request = TransactionRequestDTO.builder()
                .fromAccountNumber("account123")
                .toAccountNumber("account456")
                .amount(new BigDecimal("100"))
                .build();

        AccountEntity from = AccountEntity.builder()
                .accountNumber("account123")
                .balance(BigDecimal.valueOf(50)) // 부족한 잔액
                .build();

        AccountEntity to = AccountEntity.builder()
                .accountNumber("account456")
                .balance(BigDecimal.ZERO)
                .build();

        when(accountRepository.findByAccountNumber("account123")).thenReturn(Optional.of(from));
        when(accountRepository.findByAccountNumber("account456")).thenReturn(Optional.of(to));
        when(transferPolicy.calculateFee(any())).thenReturn(new BigDecimal("10"));
        when(transferPolicy.getTransferDailyLimit()).thenReturn(BigDecimal.valueOf(1000000));

        assertThrows(TransferSystemException.class, () -> transactionService.transfer(request));
    }
}