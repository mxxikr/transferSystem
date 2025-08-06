package com.transfer.system.service;

import com.transfer.system.domain.AccountEntity;
import com.transfer.system.domain.TransactionEntity;
import com.transfer.system.dto.TransactionRequestDTO;
import com.transfer.system.dto.TransactionResponseDTO;
import com.transfer.system.enums.AccountStatus;
import com.transfer.system.enums.CurrencyType;
import com.transfer.system.enums.TransactionType;
import com.transfer.system.exception.ErrorCode;
import com.transfer.system.exception.TransferSystemException;
import com.transfer.system.policy.PagingPolicy;
import com.transfer.system.policy.TransferPolicy;
import com.transfer.system.repository.AccountRepository;
import com.transfer.system.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionServiceTest {
    private AccountRepository accountRepository;
    private TransactionRepository transactionRepository;
    private TransferPolicy transferPolicy;
    private PagingPolicy pagingPolicy;
    private TransactionServiceImpl transactionService;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        transactionRepository = mock(TransactionRepository.class);
        transferPolicy = mock(TransferPolicy.class);
        pagingPolicy = mock(PagingPolicy.class);
        transactionService = new TransactionServiceImpl(accountRepository, transactionRepository, transferPolicy, pagingPolicy);
    }

    // ==================== 이체 테스트 ====================
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

        when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.of(fromAccountEntity));
        when(accountRepository.findByAccountNumberLock("account456")).thenReturn(Optional.of(toAccountEntity));

        when(transferPolicy.calculateFee(amount)).thenReturn(fee);
        when(transferPolicy.getTransferDailyLimit()).thenReturn(new BigDecimal("3000000"));

        when(transactionRepository.getTodayTransferTotalFromAccount("account123")).thenReturn(BigDecimal.ZERO);

        when(transactionRepository.save(any(TransactionEntity.class))).thenReturn(savedTransaction);

        // when / then
        assertDoesNotThrow(() -> transactionService.transfer(transactionRequestDTO));
        verify(transactionRepository).save(any(TransactionEntity.class));
    }

    /**
     * 이체 테스트 - null DTO
     */
    @Test
    void transfer_nullDTO_Throw() {
        TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
            transactionService.transfer(null));

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    /**
     * 이체 테스트 - null 출금 계좌번호
     */
    @Test
    void transfer_nullFromAccountNumber_Throw() {
        TransactionRequestDTO transactionRequestDTO = TransactionRequestDTO.builder()
            .fromAccountNumber(null)
            .toAccountNumber("account456")
            .amount(BigDecimal.valueOf(100))
            .build();

        TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
            transactionService.transfer(transactionRequestDTO));

        assertEquals(ErrorCode.INVALID_ACCOUNT_NUMBER, exception.getErrorCode());
    }

    /**
     * 이체 테스트 - null 입금 계좌번호
     */
    @Test
    void transfer_nullToAccountNumber_Throw() {
        TransactionRequestDTO transactionRequestDTO = TransactionRequestDTO.builder()
            .fromAccountNumber("account123")
            .toAccountNumber(null)
            .amount(BigDecimal.valueOf(100))
            .build();

        TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
            transactionService.transfer(transactionRequestDTO));

        assertEquals(ErrorCode.INVALID_ACCOUNT_NUMBER, exception.getErrorCode());
    }

    /**
     * 이체 테스트 - 동일 계좌 간 이체
     */
    @Test
    void transfer_sameAccount_Throw() {
        TransactionRequestDTO transactionRequestDTO = TransactionRequestDTO.builder()
            .fromAccountNumber("account123")
            .toAccountNumber("account123")
            .amount(new BigDecimal("100"))
            .build();

        TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
            transactionService.transfer(transactionRequestDTO));

        assertEquals(ErrorCode.TRANSFER_SAME_ACCOUNT, exception.getErrorCode());
    }

    /**
     * 이체 테스트 - null 금액
     */
    @Test
    void transfer_nullAmount_Throw() {
        TransactionRequestDTO transactionRequestDTO = TransactionRequestDTO.builder()
            .fromAccountNumber("account123")
            .toAccountNumber("account456")
            .amount(null)
            .build();

        AccountEntity fromAccount = AccountEntity.builder()
            .accountNumber("account123")
            .balance(BigDecimal.valueOf(1000))
            .accountStatus(AccountStatus.ACTIVE)
            .currencyType(CurrencyType.KRW)
            .build();

        AccountEntity toAccount = AccountEntity.builder()
            .accountNumber("account456")
            .balance(BigDecimal.valueOf(500))
            .accountStatus(AccountStatus.ACTIVE)
            .currencyType(CurrencyType.KRW)
            .build();

        when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountNumberLock("account456")).thenReturn(Optional.of(toAccount));

        TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
            transactionService.transfer(transactionRequestDTO));

        assertEquals(ErrorCode.INVALID_AMOUNT, exception.getErrorCode());
    }

    /**
     * 이체 테스트 - 0원 이체
     */
    @Test
    void transfer_zeroAmount_Throw() {
        TransactionRequestDTO transactionRequestDTO = TransactionRequestDTO.builder()
            .fromAccountNumber("account123")
            .toAccountNumber("account456")
            .amount(BigDecimal.ZERO)
            .build();

        AccountEntity fromAccount = AccountEntity.builder()
            .accountNumber("account123")
            .balance(BigDecimal.valueOf(1000))
            .accountStatus(AccountStatus.ACTIVE)
            .currencyType(CurrencyType.KRW)
            .build();

        AccountEntity toAccount = AccountEntity.builder()
            .accountNumber("account456")
            .balance(BigDecimal.valueOf(500))
            .accountStatus(AccountStatus.ACTIVE)
            .currencyType(CurrencyType.KRW)
            .build();

        when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountNumberLock("account456")).thenReturn(Optional.of(toAccount));

        TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
            transactionService.transfer(transactionRequestDTO));

        assertEquals(ErrorCode.INVALID_AMOUNT, exception.getErrorCode());
    }

    /**
     * 이체 테스트 - 음수 금액
     */
    @Test
    void transfer_negativeAmount_Throw() {
        TransactionRequestDTO transactionRequestDTO = TransactionRequestDTO.builder()
            .fromAccountNumber("account123")
            .toAccountNumber("account456")
            .amount(new BigDecimal("-100"))
            .build();

        AccountEntity fromAccount = AccountEntity.builder()
            .accountNumber("account123")
            .balance(BigDecimal.valueOf(1000))
            .accountStatus(AccountStatus.ACTIVE)
            .currencyType(CurrencyType.KRW)
            .build();

        AccountEntity toAccount = AccountEntity.builder()
            .accountNumber("account456")
            .balance(BigDecimal.valueOf(500))
            .accountStatus(AccountStatus.ACTIVE)
            .currencyType(CurrencyType.KRW)
            .build();

        when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountNumberLock("account456")).thenReturn(Optional.of(toAccount));

        TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
            transactionService.transfer(transactionRequestDTO));

        assertEquals(ErrorCode.INVALID_AMOUNT, exception.getErrorCode());
    }



    /**
     * 이체 테스트 - 출금 계좌 없음
     */
    @Test
    void transfer_fromAccountNotFound_Throw() {
        TransactionRequestDTO transactionRequestDTO = TransactionRequestDTO.builder()
            .fromAccountNumber("account123")
            .toAccountNumber("account456")
            .amount(new BigDecimal("100"))
            .build();

        when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.empty());

        TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
            transactionService.transfer(transactionRequestDTO));

        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    /**
     * 이체 테스트 - 입금 계좌 없음
     */
    @Test
    void transfer_toAccountNotFound_Throw() {
        TransactionRequestDTO transactionRequestDTO = TransactionRequestDTO.builder()
            .fromAccountNumber("account123")
            .toAccountNumber("account456")
            .amount(new BigDecimal("100"))
            .build();

        AccountEntity fromAccount = AccountEntity.builder()
            .accountNumber("account123")
            .balance(BigDecimal.valueOf(5000))
            .build();

        when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountNumberLock("account456")).thenReturn(Optional.empty());

        TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
            transactionService.transfer(transactionRequestDTO));

        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
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
            .currencyType(CurrencyType.KRW)
            .build();

        AccountEntity toAccountEntity = AccountEntity.builder()
            .accountNumber("account456")
            .balance(BigDecimal.ZERO)
            .accountStatus(AccountStatus.ACTIVE)
            .currencyType(CurrencyType.KRW)
            .build();

        when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.of(fromAccountEntity));
        when(accountRepository.findByAccountNumberLock("account456")).thenReturn(Optional.of(toAccountEntity));

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

        when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.of(fromAccountEntity));
        when(accountRepository.findByAccountNumberLock("account456")).thenReturn(Optional.of(toAccountEntity));

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
                .currencyType(CurrencyType.USD)
                .balance(BigDecimal.ZERO)
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.of(fromAccountEntity));
        when(accountRepository.findByAccountNumberLock("account456")).thenReturn(Optional.of(toAccountEntity));

        TransactionRequestDTO transactionRequestDTO = TransactionRequestDTO.builder()
                .fromAccountNumber("account123")
                .toAccountNumber("account456")
                .amount(BigDecimal.valueOf(100))
                .build();

        TransferSystemException ex = assertThrows(TransferSystemException.class, () ->
                transactionService.transfer(transactionRequestDTO));

        assertEquals(ErrorCode.CURRENCY_TYPE_MISMATCH, ex.getErrorCode());
    }

    /**
     * 이체 테스트 - 수수료 null 처리
     */
    @Test
    void transfer_nullFee_Throw() {
        BigDecimal amount = new BigDecimal("100000");

        TransactionRequestDTO transactionRequestDTO = TransactionRequestDTO.builder()
                .fromAccountNumber("account123")
                .toAccountNumber("account456")
                .amount(amount)
                .build();

        AccountEntity fromAccount = AccountEntity.builder()
                .accountNumber("account123")
                .balance(new BigDecimal("200000"))
                .accountStatus(AccountStatus.ACTIVE)
                .currencyType(CurrencyType.KRW)
                .build();

        AccountEntity toAccount = AccountEntity.builder()
                .accountNumber("account456")
                .balance(new BigDecimal("500"))
                .accountStatus(AccountStatus.ACTIVE)
                .currencyType(CurrencyType.KRW)
                .build();

        when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountNumberLock("account456")).thenReturn(Optional.of(toAccount));
        when(transferPolicy.calculateFee(amount)).thenReturn(null); // null 수수료

        TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
                transactionService.transfer(transactionRequestDTO));

        assertEquals(ErrorCode.INVALID_FEE, exception.getErrorCode());
    }

    /**
     * 이체 테스트 - 수수료 음수 처리
     */
    @Test
    void transfer_negativeFee_Throw() {
        BigDecimal amount = new BigDecimal("100000");

        TransactionRequestDTO transactionRequestDTO = TransactionRequestDTO.builder()
                .fromAccountNumber("account123")
                .toAccountNumber("account456")
                .amount(amount)
                .build();

        AccountEntity fromAccount = AccountEntity.builder()
                .accountNumber("account123")
                .balance(new BigDecimal("200000"))
                .accountStatus(AccountStatus.ACTIVE)
                .currencyType(CurrencyType.KRW)
                .build();

        AccountEntity toAccount = AccountEntity.builder()
                .accountNumber("account456")
                .balance(new BigDecimal("500"))
                .accountStatus(AccountStatus.ACTIVE)
                .currencyType(CurrencyType.KRW)
                .build();

        when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.of(fromAccount));
        when(accountRepository.findByAccountNumberLock("account456")).thenReturn(Optional.of(toAccount));
        when(transferPolicy.calculateFee(amount)).thenReturn(new BigDecimal("-100")); // 음수 수수료

        TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
                transactionService.transfer(transactionRequestDTO));

        assertEquals(ErrorCode.INVALID_FEE, exception.getErrorCode());
    }

    /**
     * 이체 테스트 - 이체 한도 초과
     */
    @Test
    void transfer_limitExceeded_Throw() {
        BigDecimal amount = new BigDecimal("2990000");
        BigDecimal fee = new BigDecimal("29900");

        TransactionRequestDTO transactionRequestDTO = TransactionRequestDTO.builder()
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

        when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.of(fromAccountEntity));
        when(accountRepository.findByAccountNumberLock("account456")).thenReturn(Optional.of(toAccountEntity));
        when(transferPolicy.calculateFee(amount)).thenReturn(fee);
        when(transferPolicy.getTransferDailyLimit()).thenReturn(new BigDecimal("3000000"));
        when(transactionRepository.getTodayTransferTotalFromAccount("account123")).thenReturn(BigDecimal.ZERO);

        TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
                transactionService.transfer(transactionRequestDTO));

        assertEquals(ErrorCode.TRANSFER_LIMIT_EXCEEDED, exception.getErrorCode());
    }

    /**
     * 이체 테스트 - 잔액 부족
     */
    @Test
    void transfer_insufficientBalance_Throw() {
        BigDecimal amount = new BigDecimal("100000");
        BigDecimal fee = new BigDecimal("1000");

        TransactionRequestDTO transactionRequestDTO = TransactionRequestDTO.builder()
                .fromAccountNumber("account123")
                .toAccountNumber("account456")
                .amount(amount)
                .build();

        AccountEntity fromAccountEntity = AccountEntity.builder()
                .accountNumber("account123")
                .balance(BigDecimal.valueOf(50000)) // 부족한 잔액
                .accountStatus(AccountStatus.ACTIVE)
                .currencyType(CurrencyType.KRW)
                .build();

        AccountEntity toAccountEntity = AccountEntity.builder()
                .accountNumber("account456")
                .balance(BigDecimal.ZERO)
                .accountStatus(AccountStatus.ACTIVE)
                .currencyType(CurrencyType.KRW)
                .build();

        when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.of(fromAccountEntity));
        when(accountRepository.findByAccountNumberLock("account456")).thenReturn(Optional.of(toAccountEntity));
        when(transferPolicy.calculateFee(amount)).thenReturn(fee);
        when(transferPolicy.getTransferDailyLimit()).thenReturn(new BigDecimal("3000000"));
        when(transactionRepository.getTodayTransferTotalFromAccount("account123")).thenReturn(BigDecimal.ZERO);

        TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
                transactionService.transfer(transactionRequestDTO));

        assertEquals(ErrorCode.INSUFFICIENT_BALANCE, exception.getErrorCode());
    }

    // ========== 거래 내역 조회 테스트 ==========

    /**
     * 거래 내역 조회 테스트 - 성공
     */
    @Test
    void getTransactionHistory_success() {
        // given
        String accountNumber = "account123";
        int page = 0;
        int size = 10;

        AccountEntity account = AccountEntity.builder()
            .accountNumber(accountNumber)
            .balance(BigDecimal.valueOf(10000))
            .build();

        TransactionEntity transaction = TransactionEntity.builder()
            .transactionId(UUID.randomUUID())
            .fromAccount(account)
            .toAccount(null)
            .transactionType(TransactionType.WITHDRAW)
            .amount(BigDecimal.valueOf(1000))
            .fee(BigDecimal.ZERO)
            .createdTimeStamp(LocalDateTime.now())
            .build();

        Page<TransactionEntity> transactionPage = new PageImpl<>(
            List.of(transaction),
            PageRequest.of(0, 10, Sort.by("createdTimeStamp").descending()),
            1
        );

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(pagingPolicy.getValidatedPage(page)).thenReturn(0);
        when(pagingPolicy.getValidatedSize(size)).thenReturn(10);
        when(pagingPolicy.getTransactionSortField()).thenReturn("createdTimeStamp");
        when(transactionRepository.findAllByAccount(eq(account), any(Pageable.class))).thenReturn(transactionPage);

        // when & then
        assertDoesNotThrow(() -> {
            Page<TransactionResponseDTO> result = transactionService.getTransactionHistory(accountNumber, page, size);
            assertEquals(1, result.getTotalElements());
        });

        verify(accountRepository).findByAccountNumber(accountNumber);
        verify(pagingPolicy).getValidatedPage(0);
        verify(pagingPolicy).getValidatedSize(10);
        verify(transactionRepository).findAllByAccount(eq(account), any(Pageable.class));
    }

    /**
     * 거래 내역 조회 테스트 - null 계좌번호
     */
    @Test
    void getTransactionHistory_nullAccountNumber_Throw() {
        TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
                transactionService.getTransactionHistory(null, 0, 10));

        assertEquals(ErrorCode.INVALID_ACCOUNT_NUMBER, exception.getErrorCode());
    }

    /**
     * 거래 내역 조회 테스트 - 빈 계좌번호
     */
    @Test
    void getTransactionHistory_emptyAccountNumber_Throw() {
        TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
                transactionService.getTransactionHistory("  ", 0, 10));

        assertEquals(ErrorCode.INVALID_ACCOUNT_NUMBER, exception.getErrorCode());
    }

    /**
     * 거래 내역 조회 테스트 - 존재하지 않는 계좌
     */
    @Test
    void getTransactionHistory_accountNotFound_Throw() {
        String accountNumber = "account123";

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.empty());

        TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
                transactionService.getTransactionHistory(accountNumber, 0, 10));

        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    /**
     * 거래 내역 조회 테스트 - 음수 페이지 처리
     */
    @Test
    void getTransactionHistory_negativePage_success() {
        String accountNumber = "account123";
        int page = -1; // 음수 페이지
        int size = 10;

        AccountEntity account = AccountEntity.builder()
                .accountNumber(accountNumber)
                .balance(BigDecimal.valueOf(10000))
                .build();

        Page<TransactionEntity> emptyPage = new PageImpl<>(
                List.of(),
                PageRequest.of(0, 10, Sort.by("createdTimeStamp").descending()),
                0
        );

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(pagingPolicy.getValidatedPage(null)).thenReturn(0); // 음수는 null로 변환됨
        when(pagingPolicy.getValidatedSize(size)).thenReturn(10);
        when(pagingPolicy.getTransactionSortField()).thenReturn("createdTimeStamp");
        when(transactionRepository.findAllByAccount(eq(account), any(Pageable.class))).thenReturn(emptyPage);

        // when & then
        assertDoesNotThrow(() -> {
            Page<TransactionResponseDTO> result = transactionService.getTransactionHistory(accountNumber, page, size);
            assertEquals(0, result.getTotalElements());
        });
    }
}