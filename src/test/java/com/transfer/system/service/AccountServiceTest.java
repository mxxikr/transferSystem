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
    private AccountServiceImpl accountService;

    @BeforeEach
    void setUp() {
        accountRepository = mock(AccountRepository.class);
        transferPolicy = mock(TransferPolicy.class);
        accountService = new AccountServiceImpl(accountRepository, transferPolicy);
    }

    /**
     * 계좌 생성 테스트 - 계좌 번호가 중복되지 않을 때 정상 생성
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
     * 입금 테스트 - 성공
     */
    @Test
    void deposit_success() {
        // given
        AccountEntity mockAccount = AccountEntity.builder()
            .accountNumber("account123")
            .balance(BigDecimal.valueOf(1000))
            .build();

        when(accountRepository.findByAccountNumber("account123")).thenReturn(Optional.of(mockAccount));
        when(accountRepository.save(any())).thenReturn(mockAccount);

        // when & then
        assertDoesNotThrow(() -> accountService.deposit("account123", BigDecimal.valueOf(500)));
    }

    /**
     * 입금 테스트 - 실패
     */
    @Test
    void deposit_accountNotFound_Throw() {
        // given
        when(accountRepository.findByAccountNumber("nonexist")).thenReturn(Optional.empty());

        // when & then
        assertThrows(TransferSystemException.class, () -> {
            accountService.deposit("nonexist", BigDecimal.valueOf(1000));
        });
    }

    /**
     * 출금 테스트 - 성공
     */
    @Test
    void withdraw_success() {
        // given
        AccountEntity mockAccount = AccountEntity.builder()
            .accountNumber("account123")
            .balance(BigDecimal.valueOf(1000))
            .build();

        when(accountRepository.findByAccountNumber("account123")).thenReturn(Optional.of(mockAccount));
        when(accountRepository.save(any())).thenReturn(mockAccount);

        // when & then
        assertDoesNotThrow(() -> accountService.withdraw("account123", BigDecimal.valueOf(500)));
    }

    /**
     * 출금 테스트 - 실패
     */
    @Test
    void withdraw_insufficientBalance_Throw() {
        // given
        AccountEntity mockAccount = AccountEntity.builder()
            .accountNumber("account123")
            .balance(BigDecimal.valueOf(100))
            .build();

        when(accountRepository.findByAccountNumber("account123")).thenReturn(Optional.of(mockAccount));

        // when & then
        assertThrows(TransferSystemException.class, () -> accountService.withdraw("account123", BigDecimal.valueOf(500)));
    }

    /**
     * 출금 테스트 - 출금 한도 초과 시 예외 발생
     */
    @Test
    void withdraw_exceedsDailyLimit_Throw() {
        // given
        BigDecimal overLimitAmount = new BigDecimal("1000001"); // 출금 정책 한도: 1000000
        AccountEntity mockAccount = AccountEntity.builder()
            .accountNumber("account123")
            .balance(overLimitAmount.add(BigDecimal.valueOf(1000))) // 충분한 잔액
            .build();

        when(accountRepository.findByAccountNumber("account123"))
                .thenReturn(Optional.of(mockAccount));

        // 정책 위반 시 예외 발생하도록 설정
        doThrow(new TransferSystemException(ErrorCode.EXCEEDS_WITHDRAW_LIMIT))
                .when(transferPolicy).validateWithdrawAmount(overLimitAmount);

        // when & then
        TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
                accountService.withdraw("account123", overLimitAmount));

        assertEquals(ErrorCode.EXCEEDS_WITHDRAW_LIMIT, exception.getErrorCode());
    }

}