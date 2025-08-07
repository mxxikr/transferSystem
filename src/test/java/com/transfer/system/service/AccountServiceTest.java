package com.transfer.system.service;

import com.transfer.system.domain.AccountEntity;
import com.transfer.system.domain.TransactionEntity;
import com.transfer.system.dto.AccountCreateRequestDTO;
import com.transfer.system.dto.AccountResponseDTO;
import com.transfer.system.enums.AccountStatus;
import com.transfer.system.enums.AccountType;
import com.transfer.system.enums.CurrencyType;
import com.transfer.system.enums.TransactionType;
import com.transfer.system.exception.ErrorCode;
import com.transfer.system.exception.TransferSystemException;
import com.transfer.system.policy.TransferPolicy;
import com.transfer.system.repository.AccountRepository;
import com.transfer.system.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransferPolicy transferPolicy;

    @Mock
    private TransactionRepository transactionRepository;

    private AccountServiceImpl accountService;

    private AccountCreateRequestDTO accountCreateRequestDTO;
    private AccountEntity accountEntity;
    private TransactionEntity mockTransaction;
    private final UUID testAccountId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        accountService = new AccountServiceImpl(accountRepository, transferPolicy, transactionRepository);

        accountCreateRequestDTO = AccountCreateRequestDTO.builder()
                .accountNumber("account123")
                .accountName("mxxikr")
                .bankName("mxxikrBank")
                .accountType(AccountType.PERSONAL)
                .currencyType(CurrencyType.KRW)
                .balance(new BigDecimal("100000"))
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        accountEntity = AccountEntity.builder()
                .accountId(testAccountId)
                .accountNumber("account123")
                .accountName("mxxikr")
                .bankName("mxxikrBank")
                .accountType(AccountType.PERSONAL)
                .currencyType(CurrencyType.KRW)
                .balance(new BigDecimal("100000"))
                .accountStatus(AccountStatus.ACTIVE)
                .build();

        mockTransaction = TransactionEntity.builder()
                .transactionId(UUID.randomUUID())
                .fromAccount(accountEntity)
                .transactionType(TransactionType.DEPOSIT)
                .amount(new BigDecimal("50000"))
                .fee(BigDecimal.ZERO)
                .createdTimeStamp(LocalDateTime.now())
                .build();
    }

    // ========================== 공통 메서드 =========================

    /**
     * 계좌 생성 시 예외 처리
     */
    private void expectCreateAccountException(AccountCreateRequestDTO dto, ErrorCode expectedError) {
        TransferSystemException exception = assertThrows(TransferSystemException.class,
                () -> accountService.createAccount(dto));
        assertEquals(expectedError, exception.getErrorCode());
    }

    /**
     * 입금 시 예외 처리
     */
    private void expectDepositException(String accountNumber, BigDecimal amount, ErrorCode expectedError) {
        TransferSystemException exception = assertThrows(TransferSystemException.class,
                () -> accountService.deposit(accountNumber, amount));
        assertEquals(expectedError, exception.getErrorCode());
    }

    /**
     * 출금 시 예외 처리
     */
    private void expectWithdrawException(String accountNumber, BigDecimal amount, ErrorCode expectedError) {
        TransferSystemException exception = assertThrows(TransferSystemException.class,
                () -> accountService.withdraw(accountNumber, amount));
        assertEquals(expectedError, exception.getErrorCode());
    }

    // ========================= 계좌 생성 테스트 =========================
    @Nested
    class CreateAccountTest {

        /**
         * 계좌 생성 성공
         */
        @Test
        void createAccount_success() {
            when(accountRepository.existsByAccountNumber("account123")).thenReturn(false);
            when(accountRepository.save(any(AccountEntity.class))).thenReturn(accountEntity);

            AccountResponseDTO result = accountService.createAccount(accountCreateRequestDTO);

            assertNotNull(result);
            assertEquals("account123", result.getAccountNumber());
            assertEquals("mxxikr", result.getAccountName());
            verify(accountRepository).save(any(AccountEntity.class));
        }

        /**
         * 계좌 생성 실패 - 요청이 null인 경우
         */
        @Test
        void createAccount_fail_whenRequestIsNull() {
            expectCreateAccountException(null, ErrorCode.INVALID_REQUEST);
            verify(accountRepository, never()).save(any());
        }

        /**
         * 계좌 생성 실패 - 중복된 계좌 번호
         */
        @Test
        void createAccount_fail_whenAccountNumberIsDuplicate() {
            when(accountRepository.existsByAccountNumber("account123")).thenReturn(true);

            expectCreateAccountException(accountCreateRequestDTO, ErrorCode.DUPLICATE_ACCOUNT_NUMBER);
            verify(accountRepository, never()).save(any());
        }

        /**
         * 계좌 생성 실패 - 계좌 번호 누락
         */
        @Test
        void createAccount_fail_whenAccountNumberIsNull() {
            AccountCreateRequestDTO invalidDto = AccountCreateRequestDTO.builder()
                    .accountNumber(null)
                    .accountName("mxxikr")
                    .bankName("mxxikrBank")
                    .accountType(AccountType.PERSONAL)
                    .currencyType(CurrencyType.KRW)
                    .balance(new BigDecimal("100000"))
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();

            expectCreateAccountException(invalidDto, ErrorCode.INVALID_REQUEST);
            verify(accountRepository, never()).save(any());
        }

        /**
         * 계좌 생성 실패 - 계좌명 누락
         */
        @Test
        void createAccount_fail_whenAccountNameIsNull() {
            AccountCreateRequestDTO invalidDto = AccountCreateRequestDTO.builder()
                    .accountNumber("account123")
                    .accountName(null)
                    .bankName("mxxikrBank")
                    .accountType(AccountType.PERSONAL)
                    .currencyType(CurrencyType.KRW)
                    .balance(new BigDecimal("100000"))
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();

            expectCreateAccountException(invalidDto, ErrorCode.INVALID_REQUEST);
            verify(accountRepository, never()).save(any());
        }

        /**
         * 계좌 생성 실패 - 은행명 누락
         */
        @Test
        void createAccount_fail_whenBankNameIsNull() {
            AccountCreateRequestDTO invalidDto = AccountCreateRequestDTO.builder()
                    .accountNumber("account123")
                    .accountName("mxxikr")
                    .bankName(null)
                    .accountType(AccountType.PERSONAL)
                    .currencyType(CurrencyType.KRW)
                    .balance(new BigDecimal("100000"))
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();

            expectCreateAccountException(invalidDto, ErrorCode.INVALID_REQUEST);
            verify(accountRepository, never()).save(any());
        }

        /**
         * 계좌 생성 실패 - 계좌 유형 누락
         */
        @Test
        void createAccount_fail_whenAccountTypeIsNull() {
            AccountCreateRequestDTO invalidDto = AccountCreateRequestDTO.builder()
                    .accountNumber("account123")
                    .accountName("mxxikr")
                    .bankName("mxxikrBank")
                    .accountType(null)
                    .currencyType(CurrencyType.KRW)
                    .balance(new BigDecimal("100000"))
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();

            expectCreateAccountException(invalidDto, ErrorCode.INVALID_REQUEST);
            verify(accountRepository, never()).save(any());
        }

        /**
         * 계좌 생성 실패 - 통화 종류 누락
         */
        @Test
        void createAccount_fail_whenCurrencyTypeIsNull() {
            AccountCreateRequestDTO invalidDto = AccountCreateRequestDTO.builder()
                    .accountNumber("account123")
                    .accountName("mxxikr")
                    .bankName("mxxikrBank")
                    .accountType(AccountType.PERSONAL)
                    .currencyType(null)
                    .balance(new BigDecimal("100000"))
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();

            expectCreateAccountException(invalidDto, ErrorCode.INVALID_REQUEST);
            verify(accountRepository, never()).save(any());
        }

        /**
         * 계좌 생성 실패 - 잔액 누락
         */
        @Test
        void createAccount_fail_whenBalanceIsNull() {
            AccountCreateRequestDTO invalidDto = AccountCreateRequestDTO.builder()
                    .accountNumber("account123")
                    .accountName("mxxikr")
                    .bankName("mxxikrBank")
                    .accountType(AccountType.PERSONAL)
                    .currencyType(CurrencyType.KRW)
                    .balance(null)
                    .accountStatus(AccountStatus.ACTIVE)
                    .build();

            expectCreateAccountException(invalidDto, ErrorCode.INVALID_REQUEST);
            verify(accountRepository, never()).save(any());
        }
    }

    // ========================= 계좌 조회 테스트 =========================
    @Nested
    class GetAccountTest {

        /**
         * 계좌 조회 성공
         */
        @Test
        void getAccount_success() {
            when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(accountEntity));

            AccountResponseDTO result = accountService.getAccount(testAccountId);

            assertNotNull(result);
            assertEquals(testAccountId, result.getAccountId());
            assertEquals("account123", result.getAccountNumber());
            verify(accountRepository).findById(testAccountId);
        }

        /**
         * 계좌 조회 실패 - 계좌가 존재하지 않는 경우
         */
        @Test
        void getAccount_fail_whenAccountNotFound() {
            when(accountRepository.findById(testAccountId)).thenReturn(Optional.empty());

            TransferSystemException exception = assertThrows(TransferSystemException.class,
                    () -> accountService.getAccount(testAccountId));
            assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
        }
    }

    // ========================= 계좌 삭제 테스트 =========================
    @Nested
    class DeleteAccountTest {

        /**
         * 계좌 삭제 성공
         */
        @Test
        void deleteAccount_success() {
            when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(accountEntity));

            assertDoesNotThrow(() -> accountService.deleteAccount(testAccountId));
            verify(accountRepository).delete(accountEntity);
        }

        /**
         * 계좌 삭제 실패 - 계좌가 존재하지 않는 경우
         */
        @Test
        void deleteAccount_fail_whenAccountNotFound() {
            when(accountRepository.findById(testAccountId)).thenReturn(Optional.empty());

            TransferSystemException exception = assertThrows(TransferSystemException.class,
                    () -> accountService.deleteAccount(testAccountId));
            assertEquals(ErrorCode.ACCOUNT_NOT_FOUND, exception.getErrorCode());
            verify(accountRepository, never()).delete(any());
        }
    }

    // ========================= 입금 테스트 =========================
    @Nested
    class DepositTest {

        /**
         * 입금 성공
         */
        @Test
        void deposit_success() {
            when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.of(accountEntity));
            when(transactionRepository.save(any())).thenReturn(mockTransaction);

            assertDoesNotThrow(() -> accountService.deposit("account123", new BigDecimal("50000")));

            verify(accountRepository).save(accountEntity);
            verify(transactionRepository).save(any(TransactionEntity.class));
        }

        /**
         * 입금 실패 - 계좌가 존재하지 않는 경우
         */
        @Test
        void deposit_fail_whenAccountNotFound() {
            when(accountRepository.findByAccountNumberLock("nonexistent")).thenReturn(Optional.empty());

            expectDepositException("nonexistent", new BigDecimal("50000"), ErrorCode.ACCOUNT_NOT_FOUND);
            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        /**
         * 입금 실패 - null 계좌번호
         */
        @Test
        void deposit_fail_whenAccountNumberIsNull() {
            expectDepositException(null, new BigDecimal("50000"), ErrorCode.INVALID_REQUEST);
            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        /**
         * 입금 실패 - null 금액
         */
        @Test
        void deposit_fail_whenAmountIsNull() {
            expectDepositException("account123", null, ErrorCode.INVALID_REQUEST);
            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        /**
         * 입금 실패 - 음수 금액
         */
        @Test
        void deposit_fail_whenAmountIsNegative() {
            expectDepositException("account123", new BigDecimal("-1000"), ErrorCode.INVALID_REQUEST);
            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        /**
         * 입금 실패 - 0원 금액
         */
        @Test
        void deposit_fail_whenAmountIsZero() {
            expectDepositException("account123", BigDecimal.ZERO, ErrorCode.INVALID_REQUEST);
            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }
    }

    // ========================= 출금 테스트 =========================
    @Nested
    class WithdrawTest {

        /**
         * 출금 성공
         */
        @Test
        void withdraw_success() {
            TransactionEntity withdrawTransaction = TransactionEntity.builder()
                    .transactionId(UUID.randomUUID())
                    .fromAccount(accountEntity)
                    .transactionType(TransactionType.WITHDRAW)
                    .amount(new BigDecimal("30000"))
                    .fee(BigDecimal.ZERO)
                    .createdTimeStamp(LocalDateTime.now())
                    .build();

            when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.of(accountEntity));
            when(transactionRepository.getTodayWithdrawTotalFromAccount("account123")).thenReturn(BigDecimal.ZERO);
            when(transactionRepository.save(any())).thenReturn(withdrawTransaction);
            doNothing().when(transferPolicy).validateWithdrawAmount(anyString(), any(BigDecimal.class), any(BigDecimal.class));

            assertDoesNotThrow(() -> accountService.withdraw("account123", new BigDecimal("30000")));

            verify(accountRepository).save(accountEntity);
            verify(transactionRepository).save(any(TransactionEntity.class));
        }

        /**
         * 출금 실패 - 계좌가 존재하지 않는 경우
         */
        @Test
        void withdraw_fail_whenAccountNotFound() {
            when(accountRepository.findByAccountNumberLock("nonexistent")).thenReturn(Optional.empty());

            expectWithdrawException("nonexistent", new BigDecimal("30000"), ErrorCode.ACCOUNT_NOT_FOUND);
            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        /**
         * 출금 실패 - 잔액 부족
         */
        @Test
        void withdraw_fail_whenInsufficientBalance() {
            when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.of(accountEntity));
            when(transactionRepository.getTodayWithdrawTotalFromAccount("account123")).thenReturn(BigDecimal.ZERO);
            doNothing().when(transferPolicy).validateWithdrawAmount(anyString(), any(BigDecimal.class), any(BigDecimal.class));

            expectWithdrawException("account123", new BigDecimal("200000"), ErrorCode.INSUFFICIENT_BALANCE);
            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        /**
         * 출금 실패 - 일일 한도 초과
         */
        @Test
        void withdraw_fail_whenExceedsDailyLimit() {
            when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.of(accountEntity));
            when(transactionRepository.getTodayWithdrawTotalFromAccount("account123")).thenReturn(new BigDecimal("500000"));
            doThrow(new TransferSystemException(ErrorCode.EXCEEDS_WITHDRAW_LIMIT))
                    .when(transferPolicy).validateWithdrawAmount(eq("account123"), eq(new BigDecimal("600000")), eq(new BigDecimal("500000")));

            expectWithdrawException("account123", new BigDecimal("600000"), ErrorCode.EXCEEDS_WITHDRAW_LIMIT);
            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        /**
         * 출금 실패 - null 계좌번호
         */
        @Test
        void withdraw_fail_whenAccountNumberIsNull() {
            expectWithdrawException(null, new BigDecimal("30000"), ErrorCode.INVALID_REQUEST);
            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        /**
         * 출금 실패 - null 금액
         */
        @Test
        void withdraw_fail_whenAmountIsNull() {
            expectWithdrawException("account123", null, ErrorCode.INVALID_REQUEST);
            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        /**
         * 출금 실패 - 음수 금액
         */
        @Test
        void withdraw_fail_whenAmountIsNegative() {
            expectWithdrawException("account123", new BigDecimal("-1000"), ErrorCode.INVALID_REQUEST);
            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }

        /**
         * 출금 실패 - 0원 금액
         */
        @Test
        void withdraw_fail_whenAmountIsZero() {
            expectWithdrawException("account123", BigDecimal.ZERO, ErrorCode.INVALID_REQUEST);
            verify(accountRepository, never()).save(any());
            verify(transactionRepository, never()).save(any());
        }
    }
}