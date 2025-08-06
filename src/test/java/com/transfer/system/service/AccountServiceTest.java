package com.transfer.system.service;

import com.transfer.system.domain.AccountEntity;
import com.transfer.system.dto.AccountCreateRequestDTO;
import com.transfer.system.enums.AccountStatus;
import com.transfer.system.enums.AccountType;
import com.transfer.system.enums.CurrencyType;
import com.transfer.system.exception.ErrorCode;
import com.transfer.system.exception.TransferSystemException;
import com.transfer.system.policy.TransferPolicy;
import com.transfer.system.repository.AccountRepository;
import com.transfer.system.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountServiceTest {

    private AccountRepository accountRepository;
    private TransferPolicy transferPolicy;
    private TransactionRepository transactionRepository;
    private AccountServiceImpl accountService;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        transferPolicy = mock(TransferPolicy.class);
        transactionRepository = mock(TransactionRepository.class);
        accountService = new AccountServiceImpl(accountRepository, transferPolicy, transactionRepository);
    }

    // ==================== 계좌 생성 테스트 ====================

    /**
     * 계좌 생성 테스트 - 정상 생성
     */
    @Test
    void createAccount_success() {
        // given
        AccountCreateRequestDTO accountCreateRequestDTO = AccountCreateRequestDTO.builder()
            .accountNumber("account123")
            .accountName("mxxikr")
            .bankName("mxxikrBank")
            .accountType(AccountType.PERSONAL)
            .currencyType(CurrencyType.KRW)
            .balance(new BigDecimal("10000.00"))
            .accountStatus(AccountStatus.ACTIVE)
            .build();

        when(accountRepository.existsByAccountNumber("account123")).thenReturn(false);
        when(accountRepository.save(any(AccountEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // when & then
        assertDoesNotThrow(() -> accountService.createAccount(accountCreateRequestDTO));
    }

    /**
     * 계좌 생성 테스트 - null DTO
     */
    @Test
    void createAccount_nullDTO_Throw() {
        // when & then
        TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
            accountService.createAccount(null));

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    /**
     * 계좌 생성 테스트 - 계좌 번호가 중복될 때 예외 발생
     */
    @Test
    void createAccount_duplicateAccountNumber_Throw() {
        // given
        when(accountRepository.existsByAccountNumber("account123")).thenReturn(true);

        AccountCreateRequestDTO accountCreateRequestDTO = AccountCreateRequestDTO.builder()
            .accountNumber("account123")
            .accountName("mxxikr")
            .bankName("mxxikrBank")
            .accountType(AccountType.PERSONAL)
            .currencyType(CurrencyType.KRW)
            .balance(new BigDecimal("10000.00"))
            .accountStatus(AccountStatus.ACTIVE)
            .build();

        // when
        TransferSystemException exception = assertThrows(TransferSystemException.class, () -> {
            accountService.createAccount(accountCreateRequestDTO);
        });

        // then
        assertEquals(ErrorCode.DUPLICATE_ACCOUNT_NUMBER, exception.getErrorCode());
    }

    /**
     * 계좌 생성 테스트 - 계좌 번호 누락
     */
    @Test
    void createAccount_nullRequiredField_Throw() {
        AccountCreateRequestDTO accountCreateRequestDTO = AccountCreateRequestDTO.builder()
            .accountNumber(null)
            .accountName("mxxikr")
            .bankName("mxxikrBank")
            .accountType(AccountType.PERSONAL)
            .currencyType(CurrencyType.KRW)
            .balance(BigDecimal.TEN)
            .build();

        TransferSystemException ex = assertThrows(TransferSystemException.class, () ->
            accountService.createAccount(accountCreateRequestDTO));

        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    /**
     * 계좌 생성 테스트 - 계좌명 누락
     */
    @Test
    void createAccount_nullAccountName_Throw() {
        AccountCreateRequestDTO accountCreateRequestDTO = AccountCreateRequestDTO.builder()
            .accountNumber("account123")
            .accountName(null)
            .bankName("mxxikrBank")
            .accountType(AccountType.PERSONAL)
            .currencyType(CurrencyType.KRW)
            .balance(BigDecimal.TEN)
            .build();

        TransferSystemException ex = assertThrows(TransferSystemException.class, () ->
            accountService.createAccount(accountCreateRequestDTO));

        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    /**
     * 계좌 생성 테스트 - 은행명 누락
     */
    @Test
    void createAccount_nullBankName_Throw() {
        AccountCreateRequestDTO accountCreateRequestDTO = AccountCreateRequestDTO.builder()
            .accountNumber("account123")
            .accountName("mxxikr")
            .bankName(null)
            .accountType(AccountType.PERSONAL)
            .currencyType(CurrencyType.KRW)
            .balance(BigDecimal.TEN)
            .build();

        TransferSystemException ex = assertThrows(TransferSystemException.class, () ->
            accountService.createAccount(accountCreateRequestDTO));

        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    /**
     * 계좌 생성 테스트 - 계좌 유형 누락
     */
    @Test
    void createAccount_nullAccountType_Throw() {
        AccountCreateRequestDTO accountCreateRequestDTO = AccountCreateRequestDTO.builder()
            .accountNumber("account123")
            .accountName("mxxikr")
            .bankName("mxxikrBank")
            .accountType(null)
            .currencyType(CurrencyType.KRW)
            .balance(BigDecimal.TEN)
            .build();

        TransferSystemException ex = assertThrows(TransferSystemException.class, () ->
            accountService.createAccount(accountCreateRequestDTO));

        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    /**
     * 계좌 생성 테스트 - 통화 종류 누락
     */
    @Test
    void createAccount_nullCurrencyType_Throw() {
        AccountCreateRequestDTO accountCreateRequestDTO = AccountCreateRequestDTO.builder()
            .accountNumber("account123")
            .accountName("mxxikr")
            .bankName("mxxikrBank")
            .accountType(AccountType.PERSONAL)
            .currencyType(null)
            .balance(BigDecimal.TEN)
            .build();

        TransferSystemException ex = assertThrows(TransferSystemException.class, () ->
            accountService.createAccount(accountCreateRequestDTO));

        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    /**
     * 계좌 생성 테스트 - 잔액 누락
     */
    @Test
    void createAccount_nullBalance_Throw() {
        AccountCreateRequestDTO accountCreateRequestDTO = AccountCreateRequestDTO.builder()
            .accountNumber("account123")
            .accountName("mxxikr")
            .bankName("mxxikrBank")
            .accountType(AccountType.PERSONAL)
            .currencyType(CurrencyType.KRW)
            .balance(null)
            .build();

        TransferSystemException ex = assertThrows(TransferSystemException.class, () ->
            accountService.createAccount(accountCreateRequestDTO));

        assertEquals(ErrorCode.INVALID_REQUEST, ex.getErrorCode());
    }

    // ==================== 계좌 조회 테스트 ====================
    /**
     * 계좌 조회 테스트 - 성공
     */
    @Test
    void getAccount_success() {
        // given
        UUID id = UUID.randomUUID();
        AccountEntity mockAccount = AccountEntity.builder()
            .accountId(id)
            .accountNumber("account123")
            .accountName("mxxikr")
            .bankName("mxxikrBank")
            .accountType(AccountType.PERSONAL)
            .currencyType(CurrencyType.KRW)
            .balance(BigDecimal.valueOf(10000))
            .accountStatus(AccountStatus.ACTIVE)
            .build();

        when(accountRepository.findById(id)).thenReturn(Optional.of(mockAccount));

        // when & then
        assertDoesNotThrow(() -> accountService.getAccount(id));
    }

    /**
     * 계좌 조회 테스트 - 실패
     */
    @Test
    void getAccount_notFound_Throw() {
        // given
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        // when & then
        assertThrows(TransferSystemException.class, () -> accountService.getAccount(accountId));
    }

    // ==================== 계좌 삭제 테스트 ====================
    /**
     * 계좌 삭제 테스트 - 성공
     */
    @Test
    void deleteAccount_success() {
        // given
        UUID accountId = UUID.randomUUID();
        AccountEntity accountEntity = AccountEntity.builder().accountId(accountId).build();
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(accountEntity));

        // when & then
        assertDoesNotThrow(() -> accountService.deleteAccount(accountId));
        verify(accountRepository).delete(accountEntity);
    }

    /**
     * 계좌 삭제 테스트 - 존재하지 않는 계좌
     */
    @Test
    void deleteAccount_notFound_Throw() {
        // given
        UUID accountId = UUID.randomUUID();
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        // when & then
        TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
            accountService.deleteAccount(accountId));

        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    // ==================== 입금 테스트 ====================
    /**
     * 입금 테스트 - 성공
     */
    @Test
    void deposit_success() {
        // given
        AccountEntity mockAccount = AccountEntity.builder()
            .accountNumber("account123")
            .balance(BigDecimal.valueOf(1000))
            .build();

        when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.of(mockAccount));
        when(accountRepository.save(any())).thenReturn(mockAccount);

        // when & then
        assertDoesNotThrow(() -> accountService.deposit("account123", BigDecimal.valueOf(500)));
    }

    /**
     * 입금 테스트 - 존재하지 않는 계좌
     */
    @Test
    void deposit_accountNotFound_Throw() {
        // given
        when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.empty());

        // when & then
        assertThrows(TransferSystemException.class, () -> {
            accountService.deposit("account123", BigDecimal.valueOf(1000));
        });
    }

    /**
     * 입금 테스트 - null 계좌번호
     */
    @Test
    void deposit_nullAccountNumber_Throw() {
        TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
            accountService.deposit(null, BigDecimal.valueOf(1000)));

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    /**
     * 입금 테스트 - null 금액
     */
    @Test
    void deposit_nullAmount_Throw() {
        TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
            accountService.deposit("account123", null));

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    /**
     * 입금 테스트 - 음수 금액
     */
    @Test
    void deposit_negativeAmount_Throw() {
        TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
            accountService.deposit("account123", BigDecimal.valueOf(-100)));

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    /**
     * 입금 테스트 - 0 금액
     */
    @Test
    void deposit_zeroAmount_Throw() {
        TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
            accountService.deposit("account123", BigDecimal.ZERO));

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    // ==================== 출금 테스트 ====================
    /**
     * 출금 테스트 - 성공
     */
    @Test
    void withdraw_success() {
        // given
        String accountNumber = "account123";
        BigDecimal withdrawAmount = BigDecimal.valueOf(500);
        BigDecimal todayTotal = BigDecimal.valueOf(0);

        AccountEntity mockAccount = AccountEntity.builder()
                .accountNumber(accountNumber)
                .balance(BigDecimal.valueOf(1000))
                .build();

        when(accountRepository.findByAccountNumberLock(accountNumber)).thenReturn(Optional.of(mockAccount));
        when(accountRepository.save(any())).thenReturn(mockAccount);
        when(transactionRepository.getTodayWithdrawTotalFromAccount(accountNumber)).thenReturn(todayTotal);
        when(transactionRepository.save(any())).thenReturn(null);

        // when & then
        assertDoesNotThrow(() -> accountService.withdraw("account123", BigDecimal.valueOf(500)));
        verify(transferPolicy).validateWithdrawAmount(accountNumber, withdrawAmount, todayTotal);
    }

    /**
     * 출금 테스트 - 존재하지 않는 계좌
     */
    @Test
    void withdraw_accountNotFound_Throw() {
        // given
        when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.empty());

        // when & then
        TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
            accountService.withdraw("account123", BigDecimal.valueOf(500)));

        assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
    }

    /**
     * 출금 테스트 - 잔액 부족
     */
    @Test
    void withdraw_insufficientBalance_Throw() {
        // given
        String accountNumber = "account123";
        BigDecimal withdrawAmount = BigDecimal.valueOf(500);
        BigDecimal todayTotal = BigDecimal.valueOf(0);

        AccountEntity mockAccount = AccountEntity.builder()
            .accountNumber(accountNumber)
            .balance(BigDecimal.valueOf(100))
            .build();

        when(accountRepository.findByAccountNumberLock(accountNumber)).thenReturn(Optional.of(mockAccount));
        when(transactionRepository.getTodayWithdrawTotalFromAccount(accountNumber)).thenReturn(todayTotal);

        // when & then
        assertThrows(TransferSystemException.class, () -> accountService.withdraw(accountNumber, withdrawAmount));
    }

    /**
     * 출금 테스트 - 출금 한도 초과
     */
    @Test
    void withdraw_exceedsDailyLimit_Throw() {
        // given
        String accountNumber = "account123";
        BigDecimal overLimitAmount = new BigDecimal("1000001");
        BigDecimal todayTotal = BigDecimal.ZERO;

        AccountEntity mockAccount = AccountEntity.builder()
            .accountNumber("account123")
            .balance(overLimitAmount.add(BigDecimal.valueOf(1000)))
            .build();

        when(accountRepository.findByAccountNumberLock(accountNumber))
            .thenReturn(Optional.of(mockAccount));
        when(transactionRepository.getTodayWithdrawTotalFromAccount(accountNumber))
            .thenReturn(todayTotal);

        // 정책 위반 시 예외 발생하도록 설정
        doThrow(new TransferSystemException(ErrorCode.EXCEEDS_WITHDRAW_LIMIT))
            .when(transferPolicy).validateWithdrawAmount(accountNumber, overLimitAmount, todayTotal);

        // when & then
        TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
            accountService.withdraw("account123", overLimitAmount));

        assertEquals(ErrorCode.EXCEEDS_WITHDRAW_LIMIT, exception.getErrorCode());
        verify(transferPolicy).validateWithdrawAmount(accountNumber, overLimitAmount, todayTotal);
    }

    /**
     * 출금 테스트 - null 계좌번호
     */
    @Test
    void withdraw_nullAccountNumber_Throw() {
        TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
            accountService.withdraw(null, BigDecimal.valueOf(500)));

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    /**
     * 출금 테스트 - null 금액
     */
    @Test
    void withdraw_nullAmount_Throw() {
        TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
            accountService.withdraw("account123", null));

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    /**
     * 출금 테스트 - 음수 금액
     */
    @Test
    void withdraw_negativeAmount_Throw() {
        TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
            accountService.withdraw("account123", BigDecimal.valueOf(-100)));

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    /**
     * 출금 테스트 - 0 금액
     */
    @Test
    void withdraw_zeroAmount_Throw() {
        TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
            accountService.withdraw("account123", BigDecimal.ZERO));

        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }
}