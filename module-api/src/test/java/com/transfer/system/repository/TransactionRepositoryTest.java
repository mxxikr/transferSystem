package com.transfer.system.repository;

import com.transfer.system.domain.AccountEntity;
import com.transfer.system.domain.TransactionEntity;
import com.transfer.system.enums.AccountStatus;
import com.transfer.system.enums.AccountType;
import com.transfer.system.enums.CurrencyType;
import com.transfer.system.enums.TransactionType;
import com.transfer.system.utils.TimeUtils;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class TransactionRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TransactionRepository transactionRepository;

    private static final String testFromAccountNumber = "00125080800001";
    private static final String testToAccountNumber   = "00125080800002";

    // ==================== 테스트 유틸 ====================

    /**
     * 테스트용 계좌 생성 및 저장
     */
    private AccountEntity testAccountEntity(String accountNumber, String accountName, BigDecimal balance) {
        AccountEntity accountEntity = AccountEntity.builder()
            .accountNumber(accountNumber)
            .accountName(accountName)
            .bankName("mxxikrBank")
            .accountType(AccountType.PERSONAL)
            .currencyType(CurrencyType.KRW)
            .balance(balance)
            .accountStatus(AccountStatus.ACTIVE)
            .createdTimeStamp(TimeUtils.nowKst())
            .build();
        return entityManager.persistAndFlush(accountEntity);
    }

    /**
     * 테스트용 거래 생성 및 저장
     */
    private TransactionEntity testTransactionEntity(AccountEntity fromAccount, AccountEntity toAccount, TransactionType transactionType, BigDecimal amount, BigDecimal fee, LocalDateTime when) {
        TransactionEntity transactionEntity = TransactionEntity.builder()
            .fromAccount(fromAccount)
            .toAccount(toAccount)
            .transactionType(transactionType)
            .amount(amount)
            .fee(fee)
            .createdTimeStamp(when)
            .build();
        return entityManager.persistAndFlush(transactionEntity);
    }

    /**
     * 오늘 시작/끝 범위
     */
    private LocalDateTime startOfToday() {
        LocalDateTime now = TimeUtils.nowKst();
        return now.withHour(0).withMinute(0).withSecond(0).withNano(0);
    }

    private LocalDateTime endOfToday() {
        LocalDateTime now = TimeUtils.nowKst();
        return now.withHour(23).withMinute(59).withSecond(59).withNano(999_999_999);
    }

    // ==================== 기본 CRUD 테스트 ====================
    @Nested
    class CrudTest {

        /**
         * 거래 저장/조회 성공
         */
        @Test
        void save_and_findById() {
            AccountEntity fromAccount = testAccountEntity(testFromAccountNumber, "sender", new BigDecimal("100000"));
            AccountEntity toAccount = testAccountEntity(testToAccountNumber, "reciever", new BigDecimal("50000"));

            TransactionEntity saved = testTransactionEntity(fromAccount, toAccount, TransactionType.TRANSFER, new BigDecimal("10000"), new BigDecimal("500"), TimeUtils.nowKst());

            Optional<TransactionEntity> found = transactionRepository.findById(saved.getTransactionId());

            assertThat(found).isPresent();
            assertThat(found.get().getFromAccount().getAccountNumber()).isEqualTo(testFromAccountNumber);
            assertThat(found.get().getToAccount().getAccountNumber()).isEqualTo(testToAccountNumber);
            assertThat(found.get().getAmount()).isEqualByComparingTo("10000");
            assertThat(found.get().getFee()).isEqualByComparingTo("500");
        }

        /**
         * 존재하지 않는 ID로 조회 시
         */
        @Test
        void findById_notFound() {
            Optional<TransactionEntity> found = transactionRepository.findById(UUID.randomUUID());
            assertThat(found).isEmpty();
        }

        /**
         * 거래 삭제
         */
        @Test
        void delete_transaction() {
            AccountEntity fromAccount = testAccountEntity(testFromAccountNumber, "sender", new BigDecimal("100000"));
            AccountEntity toAccount = testAccountEntity(testToAccountNumber, "reciever", new BigDecimal("50000"));
            TransactionEntity saved = testTransactionEntity(fromAccount, toAccount, TransactionType.TRANSFER, new BigDecimal("10000"), new BigDecimal("500"), TimeUtils.nowKst());

            transactionRepository.delete(saved);
            entityManager.flush();

            assertThat(transactionRepository.findById(saved.getTransactionId())).isEmpty();
        }
    }

    // ==================== 계좌 별 거래 조회 ====================
    @Nested
    class FindAllByAccountTest {

        /**
         * 계좌별 거래 조회
         */
        @Test
        void findAllByAccount() {
            AccountEntity a1 = testAccountEntity(testFromAccountNumber, "mxxikr1", new BigDecimal("100000"));
            AccountEntity a2 = testAccountEntity(testToAccountNumber, "mxxikr2", new BigDecimal("50000"));
            AccountEntity a3 = testAccountEntity("00125080800003", "mxxikr3", new BigDecimal("70000"));

            testTransactionEntity(a1, a2, TransactionType.TRANSFER, new BigDecimal("10000"), new BigDecimal("100"), TimeUtils.nowKst());
            testTransactionEntity(a3, a1, TransactionType.TRANSFER, new BigDecimal("20000"), new BigDecimal("100"), TimeUtils.nowKst());
            testTransactionEntity(a2, a3, TransactionType.TRANSFER, new BigDecimal("5000"),  new BigDecimal("100"), TimeUtils.nowKst());

            Pageable pageable = PageRequest.of(0, 10);
            Page<TransactionEntity> page = transactionRepository.findAllByAccount(a1, pageable);

            assertThat(page.getTotalElements()).isEqualTo(2);
            assertThat(page.getContent()).allSatisfy(transactionEntity -> {
                boolean fromMatch = transactionEntity.getFromAccount() != null && testFromAccountNumber.equals(transactionEntity.getFromAccount().getAccountNumber());
                boolean toMatch   = transactionEntity.getToAccount() != null && testFromAccountNumber.equals(transactionEntity.getToAccount().getAccountNumber());
                assertThat(fromMatch || toMatch).isTrue();
            });
        }

        /**
         * 거래 내역이 없는 계좌
         */
        @Test
        void findAllByAccount_empty() {
            AccountEntity a1 = testAccountEntity(testFromAccountNumber, "a1", new BigDecimal("100000"));
            AccountEntity a2 = testAccountEntity(testToAccountNumber, "a2", new BigDecimal("50000"));
            AccountEntity empty = testAccountEntity("EMPTY", "empty", new BigDecimal("100000"));

            testTransactionEntity(a1, a2, TransactionType.TRANSFER, new BigDecimal("10000"), new BigDecimal("0"), TimeUtils.nowKst());

            Page<TransactionEntity> page = transactionRepository.findAllByAccount(empty, PageRequest.of(0, 10));
            assertThat(page.getTotalElements()).isZero();
            assertThat(page.getContent()).isEmpty();
        }

        /**
         * 페이징 크기 검증
         */
        @Test
        void findAllByAccount_paging() {
            AccountEntity fromAccount = testAccountEntity(testFromAccountNumber, "sender", new BigDecimal("1000000"));
            AccountEntity toAccount = testAccountEntity(testToAccountNumber, "reciever", new BigDecimal("50000"));

            for (int i = 0; i < 12; i++) {
                testTransactionEntity(fromAccount, toAccount, TransactionType.TRANSFER, new BigDecimal("1000"), BigDecimal.ZERO, TimeUtils.nowKst());
            }

            Page<TransactionEntity> page = transactionRepository.findAllByAccount(fromAccount, PageRequest.of(0, 5));
            assertThat(page.getContent()).hasSize(5);
            assertThat(page.getTotalElements()).isEqualTo(12);
        }
    }

    // ==================== 일일 합계 조회 ====================
    @Nested
    class SumTodayUsedAmountTest {

        /**
         * 오늘 이체 합계 조회 성공
         */
        @Test
        void sumTodayUsed_transfer() {
            AccountEntity fromAccount = testAccountEntity(testFromAccountNumber, "sender", new BigDecimal("500000"));
            AccountEntity toAccount = testAccountEntity(testToAccountNumber, "reciever", new BigDecimal("50000"));

            // 오늘 거래
            testTransactionEntity(fromAccount, toAccount, TransactionType.TRANSFER, new BigDecimal("10000"), new BigDecimal("500"), TimeUtils.nowKst());
            testTransactionEntity(fromAccount, toAccount, TransactionType.TRANSFER, new BigDecimal("20000"), new BigDecimal("1000"), TimeUtils.nowKst());

            // 어제 거래
            testTransactionEntity(fromAccount, toAccount, TransactionType.TRANSFER, new BigDecimal("99999"), BigDecimal.ZERO, TimeUtils.nowKst().minusDays(1));

            BigDecimal total = transactionRepository.getSumTodayUsedAmount(
                    testFromAccountNumber, TransactionType.TRANSFER, startOfToday(), endOfToday());

            assertThat(total).isEqualByComparingTo(new BigDecimal("30000"));
        }

        /**
         * 오늘 출금 합계 조회 성공
         */
        @Test
        void sumTodayUsed_withdraw() {
            AccountEntity fromAccount = testAccountEntity(testFromAccountNumber, "sender", new BigDecimal("500000"));

            testTransactionEntity(fromAccount, null, TransactionType.WITHDRAW, new BigDecimal("10000"), BigDecimal.ZERO, TimeUtils.nowKst());
            testTransactionEntity(fromAccount, null, TransactionType.WITHDRAW, new BigDecimal("20000"), BigDecimal.ZERO, TimeUtils.nowKst());

            BigDecimal total = transactionRepository.getSumTodayUsedAmount(testFromAccountNumber, TransactionType.WITHDRAW, startOfToday(), endOfToday());

            assertThat(total).isEqualByComparingTo(new BigDecimal("30000"));
        }

        /**
         * 거래가 없을 경우
         */
        @Test
        void sumTodayUsed_noTransactions() {
            BigDecimal total = transactionRepository.getSumTodayUsedAmount("mxxikr", TransactionType.TRANSFER, startOfToday(), endOfToday());

            assertThat(total).isEqualByComparingTo(BigDecimal.ZERO);
        }

        /**
         * 다른 유형은 포함되지 않음
         */
        @Test
        void sumTodayUsed_filtersByType() {
            AccountEntity fromAccount = testAccountEntity(testFromAccountNumber, "sender", new BigDecimal("500000"));
            AccountEntity toAccount = testAccountEntity(testToAccountNumber, "reciever", new BigDecimal("50000"));

            // TRANSFER 1건 + WITHDRAW 1건
            testTransactionEntity(fromAccount, toAccount, TransactionType.TRANSFER, new BigDecimal("7000"), BigDecimal.ZERO, TimeUtils.nowKst());
            testTransactionEntity(fromAccount, null, TransactionType.WITHDRAW, new BigDecimal("9000"), BigDecimal.ZERO, TimeUtils.nowKst());

            BigDecimal transferTotal = transactionRepository.getSumTodayUsedAmount(testFromAccountNumber, TransactionType.TRANSFER, startOfToday(), endOfToday());

            assertThat(transferTotal).isEqualByComparingTo(new BigDecimal("7000"));
        }
    }

    // ==================== 유효성/예외 ====================
    @Nested
    class ConstraintTest {

        /**
         * null 금액 저장 시 JPA 제약 예외
         */
        @Test
        void persist_nullAmount() {
            AccountEntity fromAccount = testAccountEntity(testFromAccountNumber, "sender", new BigDecimal("100000"));
            AccountEntity toAccount = testAccountEntity(testToAccountNumber, "reciever", new BigDecimal("50000"));

            TransactionEntity transactionEntity = TransactionEntity.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .transactionType(TransactionType.TRANSFER)
                .amount(null)
                .fee(new BigDecimal("10"))
                .createdTimeStamp(TimeUtils.nowKst())
                .build();

            assertThatThrownBy(() -> {
                entityManager.persist(transactionEntity);
                entityManager.flush();
            }).isInstanceOf(jakarta.persistence.PersistenceException.class);
        }

        /**
         * null 거래 유형 저장 시 JPA 제약 예외
         */
        @Test
        void persist_nullType() {
            AccountEntity fromAccount = testAccountEntity(testFromAccountNumber, "sender", new BigDecimal("100000"));
            AccountEntity toAccount = testAccountEntity(testToAccountNumber, "reciever", new BigDecimal("50000"));

            TransactionEntity transactionEntity = TransactionEntity.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .transactionType(null)
                .amount(new BigDecimal("1000"))
                .fee(BigDecimal.ZERO)
                .createdTimeStamp(TimeUtils.nowKst())
                .build();

            assertThatThrownBy(() -> {
                entityManager.persist(transactionEntity);
                entityManager.flush();
            }).isInstanceOf(ConstraintViolationException.class);
        }
    }
}