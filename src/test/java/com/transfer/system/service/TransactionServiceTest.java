
package com.transfer.system.service;

import com.transfer.system.domain.AccountEntity;
import com.transfer.system.domain.TransactionEntity;
import com.transfer.system.dto.TransactionRequestDTO;
import com.transfer.system.dto.TransactionResponseDTO;
import com.transfer.system.enums.AccountStatus;
import com.transfer.system.enums.AccountType;
import com.transfer.system.enums.CurrencyType;
import com.transfer.system.enums.TransactionType;
import com.transfer.system.exception.ErrorCode;
import com.transfer.system.exception.TransferSystemException;
import com.transfer.system.policy.PagingPolicy;
import com.transfer.system.policy.TransferPolicy;
import com.transfer.system.repository.AccountRepository;
import com.transfer.system.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private TransferPolicy transferPolicy;

    @Mock
    private PagingPolicy pagingPolicy;

    private TransactionServiceImpl transactionService;

    private TransactionRequestDTO transactionRequestDTO;
    private AccountEntity fromAccountEntity;
    private AccountEntity toAccountEntity;
    private TransactionEntity transactionEntity;

    @BeforeEach
    void setUp() {
        transactionService = new TransactionServiceImpl(
                accountRepository, transactionRepository, transferPolicy, pagingPolicy);

        transactionRequestDTO = TransactionRequestDTO.builder()
            .fromAccountNumber("account123")
            .toAccountNumber("account456")
            .amount(new BigDecimal("100000"))
            .build();

        fromAccountEntity = AccountEntity.builder()
            .accountId(UUID.randomUUID())
            .accountNumber("account123")
            .accountName("sender")
            .bankName("TestBank")
            .accountType(AccountType.PERSONAL)
            .currencyType(CurrencyType.KRW)
            .balance(new BigDecimal("200000"))
            .accountStatus(AccountStatus.ACTIVE)
            .build();

        toAccountEntity = AccountEntity.builder()
            .accountId(UUID.randomUUID())
            .accountNumber("account456")
            .accountName("receiver")
            .bankName("TestBank")
            .accountType(AccountType.PERSONAL)
            .currencyType(CurrencyType.KRW)
            .balance(new BigDecimal("100000"))
            .accountStatus(AccountStatus.ACTIVE)
            .build();

        transactionEntity = TransactionEntity.builder()
            .transactionId(UUID.randomUUID())
            .fromAccount(fromAccountEntity)
            .toAccount(toAccountEntity)
            .transactionType(TransactionType.TRANSFER)
            .amount(new BigDecimal("100000"))
            .fee(new BigDecimal("1000"))
            .createdTimeStamp(LocalDateTime.now())
            .build();
    }

    // ========================== 공통 메서드 =========================

    /**
     * 이체 시 예외 처리
     */
    private void expectTransferException(TransactionRequestDTO dto, ErrorCode expectedError) {
        TransferSystemException exception = assertThrows(TransferSystemException.class,
                () -> transactionService.transfer(dto));
        assertEquals(expectedError, exception.getErrorCode());
    }

    /**
     * 거래 내역 조회 시 예외 처리
     */
    private void expectHistoryException(String accountNumber, int page, int size, ErrorCode expectedError) {
        TransferSystemException exception = assertThrows(TransferSystemException.class,
                () -> transactionService.getTransactionHistory(accountNumber, page, size));
        assertEquals(expectedError, exception.getErrorCode());
    }

    /**
     * 이체 성공 시 공통 Mock 설정
     */
    private void setupSuccessfulTransferMocks() {
        when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.of(fromAccountEntity));
        when(accountRepository.findByAccountNumberLock("account456")).thenReturn(Optional.of(toAccountEntity));
        when(transferPolicy.calculateFee(any(BigDecimal.class))).thenReturn(new BigDecimal("1000"));
        when(transferPolicy.getTransferDailyLimit()).thenReturn(new BigDecimal("5000000"));
        when(transactionRepository.getTodayTransferTotalFromAccount("account123")).thenReturn(BigDecimal.ZERO);
        when(transactionRepository.save(any(TransactionEntity.class))).thenReturn(transactionEntity);
    }

    // ========================= 이체 테스트 =========================
    @Nested
    class TransferTest {

        /**
         * 이체 성공
         */
        @Test
        void transfer_success() {
            setupSuccessfulTransferMocks();

            TransactionResponseDTO result = transactionService.transfer(transactionRequestDTO);

            assertNotNull(result);
            assertEquals("account123", result.getFromAccountNumber());
            assertEquals("account456", result.getToAccountNumber());
            assertEquals(new BigDecimal("100000"), result.getAmount());
            verify(transactionRepository).save(any(TransactionEntity.class));
        }

        /**
         * 이체 시 null DTO
         */
        @Test
        void transfer_nullDto() {
            expectTransferException(null, ErrorCode.INVALID_REQUEST);
        }

        /**
         * 이체 시 null 출금 계좌번호
         */
        @Test
        void transfer_nullFromAccountNumber() {
            TransactionRequestDTO dto = TransactionRequestDTO.builder()
                .fromAccountNumber(null)
                .toAccountNumber("account456")
                .amount(new BigDecimal("100000"))
                .build();

            expectTransferException(dto, ErrorCode.INVALID_ACCOUNT_NUMBER);
        }

        /**
         * 이체 시 null 입금 계좌번호
         */
        @Test
        void transfer_nullToAccountNumber() {
            TransactionRequestDTO dto = TransactionRequestDTO.builder()
                .fromAccountNumber("account123")
                .toAccountNumber(null)
                .amount(new BigDecimal("100000"))
                .build();

            expectTransferException(dto, ErrorCode.INVALID_ACCOUNT_NUMBER);
        }

        /**
         * 이체 시 동일 계좌 간 이체
         */
        @Test
        void transfer_sameAccount() {
            TransactionRequestDTO dto = TransactionRequestDTO.builder()
                .fromAccountNumber("account123")
                .toAccountNumber("account123")
                .amount(new BigDecimal("100000"))
                .build();

            expectTransferException(dto, ErrorCode.TRANSFER_SAME_ACCOUNT);
        }

        /**
         * 이체 시 null 금액
         */
        @Test
        void transfer_nullAmount() {
            when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.of(fromAccountEntity));
            when(accountRepository.findByAccountNumberLock("account456")).thenReturn(Optional.of(toAccountEntity));

            TransactionRequestDTO dto = TransactionRequestDTO.builder()
                .fromAccountNumber("account123")
                .toAccountNumber("account456")
                .amount(null)
                .build();

            expectTransferException(dto, ErrorCode.INVALID_AMOUNT);
        }

        /**
         * 이체 시 0원 이체
         */
        @Test
        void transfer_zeroAmount() {
            when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.of(fromAccountEntity));
            when(accountRepository.findByAccountNumberLock("account456")).thenReturn(Optional.of(toAccountEntity));

            TransactionRequestDTO dto = TransactionRequestDTO.builder()
                .fromAccountNumber("account123")
                .toAccountNumber("account456")
                .amount(BigDecimal.ZERO)
                .build();

            expectTransferException(dto, ErrorCode.INVALID_AMOUNT);
        }

        /**
         * 이체 시 음수 금액
         */
        @Test
        void transfer_negativeAmount() {
            when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.of(fromAccountEntity));
            when(accountRepository.findByAccountNumberLock("account456")).thenReturn(Optional.of(toAccountEntity));

            TransactionRequestDTO dto = TransactionRequestDTO.builder()
                .fromAccountNumber("account123")
                .toAccountNumber("account456")
                .amount(new BigDecimal("-1000"))
                .build();

            expectTransferException(dto, ErrorCode.INVALID_AMOUNT);
        }

        /**
         * 이체 시 출금 계좌 없음
         */
        @Test
        void transfer_fromAccountNotFound() {
            when(accountRepository.findByAccountNumberLock("account456")).thenReturn(Optional.of(toAccountEntity));
            when(accountRepository.findByAccountNumberLock("nonexistent")).thenReturn(Optional.empty());

            TransactionRequestDTO dto = TransactionRequestDTO.builder()
                .fromAccountNumber("nonexistent")
                .toAccountNumber("account456")
                .amount(new BigDecimal("100000"))
                .build();

            expectTransferException(dto, ErrorCode.ACCOUNT_NOT_FOUND);
        }

        /**
         * 이체 시 입금 계좌 없음
         */
        @Test
        void transfer_toAccountNotFound() {
            when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.of(fromAccountEntity));
            when(accountRepository.findByAccountNumberLock("nonexistent")).thenReturn(Optional.empty());

            TransactionRequestDTO dto = TransactionRequestDTO.builder()
                .fromAccountNumber("account123")
                .toAccountNumber("nonexistent")
                .amount(new BigDecimal("100000"))
                .build();

            expectTransferException(dto, ErrorCode.ACCOUNT_NOT_FOUND);
        }

        /**
         * 이체 시 송신 계좌 비활성
         */
        @Test
        void transfer_fromInactiveAccount() {
            AccountEntity inactiveFromAccount = AccountEntity.builder()
                .accountId(UUID.randomUUID())
                .accountNumber("account123")
                .accountName("sender")
                .bankName("TestBank")
                .accountType(AccountType.PERSONAL)
                .currencyType(CurrencyType.KRW)
                .balance(new BigDecimal("200000"))
                .accountStatus(AccountStatus.INACTIVE)
                .build();

            when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.of(inactiveFromAccount));
            when(accountRepository.findByAccountNumberLock("account456")).thenReturn(Optional.of(toAccountEntity));

            expectTransferException(transactionRequestDTO, ErrorCode.SENDER_ACCOUNT_INACTIVE);
        }

        /**
         * 이체 시 수신 계좌 비활성
         */
        @Test
        void transfer_toInactiveAccount() {
            AccountEntity inactiveToAccount = AccountEntity.builder()
                .accountId(UUID.randomUUID())
                .accountNumber("account456")
                .accountName("receiver")
                .bankName("TestBank")
                .accountType(AccountType.PERSONAL)
                .currencyType(CurrencyType.KRW)
                .balance(new BigDecimal("100000"))
                .accountStatus(AccountStatus.INACTIVE)
                .build();

            when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.of(fromAccountEntity));
            when(accountRepository.findByAccountNumberLock("account456")).thenReturn(Optional.of(inactiveToAccount));

            expectTransferException(transactionRequestDTO, ErrorCode.RECEIVER_ACCOUNT_INACTIVE);
        }

        /**
         * 이체 시 통화 불일치
         */
        @Test
        void transfer_currencyMismatch() {
            AccountEntity usdAccount = AccountEntity.builder()
                .accountId(UUID.randomUUID())
                .accountNumber("account456")
                .accountName("receiver")
                .bankName("TestBank")
                .accountType(AccountType.PERSONAL)
                .currencyType(CurrencyType.USD)
                .balance(new BigDecimal("100000"))
                .accountStatus(AccountStatus.ACTIVE)
                .build();

            when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.of(fromAccountEntity));
            when(accountRepository.findByAccountNumberLock("account456")).thenReturn(Optional.of(usdAccount));

            expectTransferException(transactionRequestDTO, ErrorCode.CURRENCY_TYPE_MISMATCH);
        }

        /**
         * 이체 시 수수료 null 처리
         */
        @Test
        void transfer_nullFee() {
            when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.of(fromAccountEntity));
            when(accountRepository.findByAccountNumberLock("account456")).thenReturn(Optional.of(toAccountEntity));
            when(transferPolicy.calculateFee(any(BigDecimal.class))).thenReturn(null);

            expectTransferException(transactionRequestDTO, ErrorCode.INVALID_FEE);
        }

        /**
         * 이체 시 수수료 음수 처리
         */
        @Test
        void transfer_negativeFee() {
            when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.of(fromAccountEntity));
            when(accountRepository.findByAccountNumberLock("account456")).thenReturn(Optional.of(toAccountEntity));
            when(transferPolicy.calculateFee(any(BigDecimal.class))).thenReturn(new BigDecimal("-100"));

            expectTransferException(transactionRequestDTO, ErrorCode.INVALID_FEE);
        }

        /**
         * 이체 시 이체 한도 초과
         */
        @Test
        void transfer_limitExceeded() {
            when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.of(fromAccountEntity));
            when(accountRepository.findByAccountNumberLock("account456")).thenReturn(Optional.of(toAccountEntity));
            when(transferPolicy.calculateFee(any(BigDecimal.class))).thenReturn(new BigDecimal("1000"));
            when(transferPolicy.getTransferDailyLimit()).thenReturn(new BigDecimal("50000"));
            when(transactionRepository.getTodayTransferTotalFromAccount("account123")).thenReturn(BigDecimal.ZERO);

            expectTransferException(transactionRequestDTO, ErrorCode.TRANSFER_LIMIT_EXCEEDED);
        }

        /**
         * 이체 시 잔액 부족
         */
        @Test
        void transfer_insufficientBalance() {
            AccountEntity poorAccount = AccountEntity.builder()
                .accountId(UUID.randomUUID())
                .accountNumber("account123")
                .accountName("sender")
                .bankName("TestBank")
                .accountType(AccountType.PERSONAL)
                .currencyType(CurrencyType.KRW)
                .balance(new BigDecimal("50000"))
                .accountStatus(AccountStatus.ACTIVE)
                .build();

            when(accountRepository.findByAccountNumberLock("account123")).thenReturn(Optional.of(poorAccount));
            when(accountRepository.findByAccountNumberLock("account456")).thenReturn(Optional.of(toAccountEntity));
            when(transferPolicy.calculateFee(any(BigDecimal.class))).thenReturn(new BigDecimal("1000"));
            when(transferPolicy.getTransferDailyLimit()).thenReturn(new BigDecimal("5000000"));
            when(transactionRepository.getTodayTransferTotalFromAccount("account123")).thenReturn(BigDecimal.ZERO);

            expectTransferException(transactionRequestDTO, ErrorCode.INSUFFICIENT_BALANCE);
        }
    }

    // ========================= 거래 내역 조회 테스트 =========================
    @Nested
    class GetTransactionHistoryTest {

        /**
         * 거래 내역 조회 성공
         */
        @Test
        void getTransactionHistory_success() {
            String accountNumber = "account123";
            List<TransactionEntity> transactions = List.of(transactionEntity);
            Page<TransactionEntity> transactionPage = new PageImpl<>(transactions, PageRequest.of(0, 10), transactions.size());

            when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(fromAccountEntity));
            when(pagingPolicy.getValidatedPage(0)).thenReturn(0);
            when(pagingPolicy.getValidatedSize(10)).thenReturn(10);
            when(pagingPolicy.getTransactionSortField()).thenReturn("createdTimeStamp");
            when(transactionRepository.findAllByAccount(eq(fromAccountEntity), any(Pageable.class)))
                    .thenReturn(transactionPage);

            Page<TransactionResponseDTO> result = transactionService.getTransactionHistory(accountNumber, 0, 10);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
            verify(transactionRepository).findAllByAccount(eq(fromAccountEntity), any(Pageable.class));
        }

        /**
         * 거래 내역 조회 시 null 계좌번호
         */
        @Test
        void getTransactionHistory_nullAccountNumber() {
            expectHistoryException(null, 0, 10, ErrorCode.INVALID_ACCOUNT_NUMBER);
        }

        /**
         * 거래 내역 조회 시 빈 계좌번호
         */
        @Test
        void getTransactionHistory_emptyAccountNumber() {
            expectHistoryException("", 0, 10, ErrorCode.INVALID_ACCOUNT_NUMBER);
        }

        /**
         * 거래 내역 조회 시 계좌가 존재하지 않는 경우
         */
        @Test
        void getTransactionHistory_accountNotFound() {
            when(accountRepository.findByAccountNumber("nonexistent")).thenReturn(Optional.empty());

            expectHistoryException("nonexistent", 0, 10, ErrorCode.ACCOUNT_NOT_FOUND);
        }

        /**
         * 거래 내역 조회 시 음수 페이지 처리
         */
        @Test
        void getTransactionHistory_negativePage() {
            String accountNumber = "account123";
            List<TransactionEntity> transactions = List.of(transactionEntity);
            Page<TransactionEntity> transactionPage = new PageImpl<>(transactions, PageRequest.of(0, 10), transactions.size());

            when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(fromAccountEntity));
            when(pagingPolicy.getValidatedPage(null)).thenReturn(0); // 음수 페이지는 null로 처리됨
            when(pagingPolicy.getValidatedSize(10)).thenReturn(10);
            when(pagingPolicy.getTransactionSortField()).thenReturn("createdTimeStamp");
            when(transactionRepository.findAllByAccount(eq(fromAccountEntity), any(Pageable.class))).thenReturn(transactionPage);

            Page<TransactionResponseDTO> result = transactionService.getTransactionHistory(accountNumber, -1, 10);

            assertNotNull(result);
            assertEquals(1, result.getTotalElements());
        }
    }
}