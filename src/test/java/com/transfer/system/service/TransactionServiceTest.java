package com.transfer.system.service;

import com.transfer.system.domain.AccountEntity;
import com.transfer.system.domain.TransactionEntity;
import com.transfer.system.dto.TransactionRequestDTO;
import com.transfer.system.enums.AccountStatus;
import com.transfer.system.enums.CurrencyType;
import com.transfer.system.exception.ErrorCode;
import com.transfer.system.exception.TransferSystemException;
import com.transfer.system.policy.TransferPolicy;
import com.transfer.system.repository.AccountRepository;
import com.transfer.system.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

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
     * 이체 테스트 - 송, 수신 계좌 활성
     */
    @Test
    void transfer_success() {
        // given
        BigDecimal amount = new BigDecimal("100000");
        BigDecimal fee = new BigDecimal("1000");
        BigDecimal total = amount.add(fee);

        TransactionRequestDTO transactionRequestDTO = TransactionRequestDTO.builder()
            .fromAccountNumber("account123")
            .toAccountNumber("account456")
            .amount(amount)
            .build();

        AccountEntity fromAccountEntity = AccountEntity.builder() // 송신 계좌
            .accountNumber("account123")
            .balance(new BigDecimal("200000"))
            .accountStatus(AccountStatus.ACTIVE)
            .currencyType(CurrencyType.KRW)
            .build();

        AccountEntity toAccountEntity = AccountEntity.builder() // 수신 계좌
            .accountNumber("account456")
            .balance(new BigDecimal("500"))
            .accountStatus(AccountStatus.ACTIVE)
            .currencyType(CurrencyType.KRW)
            .build();

        TransactionEntity savedTransaction = TransactionEntity.builder()
            .transactionId(UUID.randomUUID())
            .fromAccount(fromAccountEntity)
            .toAccount(toAccountEntity)
            .amount(amount)
            .fee(fee)
            .createdTimeStamp(LocalDateTime.now())
            .build();

        when(accountRepository.findByAccountNumber("account123")).thenReturn(Optional.of(fromAccountEntity));
        when(accountRepository.findByAccountNumber("account456")).thenReturn(Optional.of(toAccountEntity));
        when(transferPolicy.calculateFee(amount)).thenReturn(fee);
        when(transferPolicy.getTransferDailyLimit()).thenReturn(new BigDecimal("3000000"));
        when(transactionRepository.save(any(TransactionEntity.class))).thenReturn(savedTransaction);

        // when / then
        assertDoesNotThrow(() -> transactionService.transfer(transactionRequestDTO));
        verify(transactionRepository).save(any(TransactionEntity.class));
    }

    /**
     * 이체 테스트 - 송신 계좌 비활성
     */
    @Test
    void transfer_fromInactiveAccount_Throw() {
        AccountEntity fromAccountEntity = AccountEntity.builder()
            .accountNumber("account123")
            .balance(BigDecimal.valueOf(1000))
            .accountStatus(AccountStatus.INACTIVE)
            .build();

        AccountEntity toAccountEntity = AccountEntity.builder()
            .accountNumber("account456")
            .balance(BigDecimal.ZERO)
            .accountStatus(AccountStatus.ACTIVE)
            .build();

        when(accountRepository.findByAccountNumber("account123")).thenReturn(Optional.of(fromAccountEntity));
        when(accountRepository.findByAccountNumber("account456")).thenReturn(Optional.of(toAccountEntity));

        TransactionRequestDTO transactionRequestDTO = TransactionRequestDTO.builder()
            .fromAccountNumber("account123")
            .toAccountNumber("account456")
            .amount(BigDecimal.valueOf(100))
            .build();

        TransferSystemException ex = assertThrows(TransferSystemException.class, () ->
            transactionService.transfer(transactionRequestDTO));

        assertEquals(ErrorCode.SENDER_ACCOUNT_INACTIVE, ex.getErrorCode());
    }

    /**
     * 이체 테스트 - 수신 계좌 비활성
     **/
    @Test
    void transfer_toInactiveAccount_Throw() {
        // given
        AccountEntity fromAccountEntity = AccountEntity.builder()
            .accountNumber("account123")
            .balance(new BigDecimal("10000"))
            .accountStatus(AccountStatus.ACTIVE)
            .build();

        AccountEntity toAccountEntity = AccountEntity.builder()
            .accountNumber("account456")
            .balance(BigDecimal.ZERO)
            .accountStatus(AccountStatus.INACTIVE) // 비활성 상태
            .build();

        when(accountRepository.findByAccountNumber("account123")).thenReturn(Optional.of(fromAccountEntity));
        when(accountRepository.findByAccountNumber("account456")).thenReturn(Optional.of(toAccountEntity));

        TransactionRequestDTO transactionRequestDTO =TransactionRequestDTO.builder()
            .fromAccountNumber("account123")
            .toAccountNumber("account456")
            .amount(new BigDecimal("1000"))
            .build();

        // when & then
        TransferSystemException ex = assertThrows(TransferSystemException.class, () ->
            transactionService.transfer(transactionRequestDTO));

        assertEquals(ErrorCode.RECEIVER_ACCOUNT_INACTIVE, ex.getErrorCode());
    }

    /**
     * 이체 테스트 - 동일 계좌 간 이체
     */
    @Test
    void transfer_sameAccount_Throw() {
        TransactionRequestDTO transactionRequestDTO =TransactionRequestDTO.builder()
            .fromAccountNumber("account123")
            .toAccountNumber("account123")
            .amount(new BigDecimal("100"))
            .build();

        assertThrows(TransferSystemException.class, () -> transactionService.transfer(transactionRequestDTO));
    }

    /**
     * 이체 테스트 - 출금 계좌 없음
     */
    @Test
    void transfer_fromAccountNotFound_Throw() {
        TransactionRequestDTO transactionRequestDTO =TransactionRequestDTO.builder()
            .fromAccountNumber("account123")
            .toAccountNumber("account456")
            .amount(new BigDecimal("100"))
            .build();

        when(accountRepository.findByAccountNumber("account123")).thenReturn(Optional.empty());

        assertThrows(TransferSystemException.class, () -> transactionService.transfer(transactionRequestDTO));
    }

    /**
     * 이체 테스트 - 입금 계좌 없음
     */
    @Test
    void transfer_toAccountNotFound_Throw() {
        TransactionRequestDTO transactionRequestDTO =TransactionRequestDTO.builder()
            .fromAccountNumber("account123")
            .toAccountNumber("account456")
            .amount(new BigDecimal("100"))
            .build();

        when(accountRepository.findByAccountNumber("account123")).thenReturn(Optional.of(AccountEntity.builder()
            .accountNumber("account123")
            .balance(BigDecimal.valueOf(5000))
            .build()));
        when(accountRepository.findByAccountNumber("account456")).thenReturn(Optional.empty());

        assertThrows(TransferSystemException.class, () -> transactionService.transfer(transactionRequestDTO));
    }

    /**
     * 이체 테스트 - 수수료 포함 시 이체 한도 초과
     */
    @Test
    void transfer_limitExceeded_Throw() {
        BigDecimal amount = new BigDecimal("2990000");
        BigDecimal fee = new BigDecimal("29900");
        BigDecimal total = amount.add(fee);

        TransactionRequestDTO transactionRequestDTO =TransactionRequestDTO.builder()
            .fromAccountNumber("account123")
            .toAccountNumber("account456")
            .amount(amount)
            .build();

        AccountEntity fromAccountEntity = AccountEntity.builder()
            .accountNumber("account123")
            .balance(BigDecimal.valueOf(5000000))
            .accountStatus(AccountStatus.ACTIVE)
            .currencyType(CurrencyType.KRW)
            .build();

        AccountEntity toAccountEntity = AccountEntity.builder()
            .accountNumber("account456")
            .balance(BigDecimal.ZERO)
            .accountStatus(AccountStatus.ACTIVE)
            .currencyType(CurrencyType.KRW)
            .build();

        when(accountRepository.findByAccountNumber("account123")).thenReturn(Optional.of(fromAccountEntity));
        when(accountRepository.findByAccountNumber("account456")).thenReturn(Optional.of(toAccountEntity));
        when(transferPolicy.calculateFee(amount)).thenReturn(fee);
        when(transferPolicy.getTransferDailyLimit()).thenReturn(new BigDecimal("3000000"));

        assertThrows(TransferSystemException.class, () -> transactionService.transfer(transactionRequestDTO));
    }

    /**
     * 이체 테스트 - 수수료 포함 시 잔액 부족
     */
    @Test
    void transfer_insufficientBalance_Throw() {
        BigDecimal amount = new BigDecimal("100000");
        BigDecimal fee = new BigDecimal("1000"); 
        BigDecimal total = amount.add(fee);

        TransactionRequestDTO transactionRequestDTO = TransactionRequestDTO.builder()
            .fromAccountNumber("account123")
            .toAccountNumber("account456")
            .amount(amount)
            .build();

        AccountEntity fromAccountEntity = AccountEntity.builder()
            .accountNumber("account123")
            .balance(BigDecimal.valueOf(100000))
            .accountStatus(AccountStatus.ACTIVE)
            .currencyType(CurrencyType.KRW)
            .build();

        AccountEntity toAccountEntity = AccountEntity.builder()
            .accountNumber("account456")
            .balance(BigDecimal.ZERO)
            .accountStatus(AccountStatus.ACTIVE)
            .currencyType(CurrencyType.KRW)
            .build();

        when(accountRepository.findByAccountNumber("account123")).thenReturn(Optional.of(fromAccountEntity));
        when(accountRepository.findByAccountNumber("account456")).thenReturn(Optional.of(toAccountEntity));
        when(transferPolicy.calculateFee(amount)).thenReturn(fee);
        when(transferPolicy.getTransferDailyLimit()).thenReturn(new BigDecimal("3000000"));

        assertThrows(TransferSystemException.class, () -> transactionService.transfer(transactionRequestDTO));
    }

    /**
     * 이체 테스트 - 음수, 0원 이체
     */
    @Test
    void transfer_zeroOrNegativeAmount_Throw() {
        TransactionRequestDTO zeroTransactionRequestDTO = TransactionRequestDTO.builder()
            .fromAccountNumber("account123")
            .toAccountNumber("account456")
            .amount(BigDecimal.ZERO)
            .build();

        TransactionRequestDTO negativeTransactionRequestDTO = TransactionRequestDTO.builder()
            .fromAccountNumber("account123")
            .toAccountNumber("account456")
            .amount(new BigDecimal("-100"))
            .build();

        assertThrows(TransferSystemException.class, () -> transactionService.transfer(zeroTransactionRequestDTO));
        assertThrows(TransferSystemException.class, () -> transactionService.transfer(negativeTransactionRequestDTO));
    }

    /**
     * 이체 테스트 - 통화 불일치
     */
    @Test
    void transfer_currencyMismatch_Throw() {
        AccountEntity fromAccountEntity = AccountEntity.builder()
            .accountNumber("account123")
            .currencyType(CurrencyType.KRW)
            .balance(BigDecimal.valueOf(10000))
            .accountStatus(AccountStatus.ACTIVE)
            .build();

        AccountEntity toAccountEntity = AccountEntity.builder()
            .accountNumber("account456")
            .currencyType(CurrencyType.USD) // 통화 불일치
            .balance(BigDecimal.ZERO)
            .accountStatus(AccountStatus.ACTIVE)
            .build();

        when(accountRepository.findByAccountNumber("account123")).thenReturn(Optional.of(fromAccountEntity));
        when(accountRepository.findByAccountNumber("account456")).thenReturn(Optional.of(toAccountEntity));

        TransactionRequestDTO transactionRequestDTO = TransactionRequestDTO.builder()
            .fromAccountNumber("account123")
            .toAccountNumber("account456")
            .amount(BigDecimal.valueOf(100))
            .build();

        TransferSystemException ex = assertThrows(TransferSystemException.class,
                () -> transactionService.transfer(transactionRequestDTO));

        assertEquals(ErrorCode.CURRENCY_TYPE_MISMATCH, ex.getErrorCode());
    }
}