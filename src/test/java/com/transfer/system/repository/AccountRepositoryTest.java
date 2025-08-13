
package com.transfer.system.repository;

import com.transfer.system.domain.AccountEntity;
import com.transfer.system.enums.AccountStatus;
import com.transfer.system.enums.AccountType;
import com.transfer.system.enums.CurrencyType;
import com.transfer.system.utils.TimeUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class AccountRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AccountRepository accountRepository;

    private static final String testFromAccountNumber = "00125080800001";
    private static final String testToAccountNumber = "00125080800002";

    /**
     * 테스트용 계좌
     */
    private AccountEntity createTestAccount(String accountNumber, String accountName, BigDecimal balance) {
        return AccountEntity.builder()
            .accountNumber(accountNumber)
            .accountName(accountName)
            .bankName("mxxikrBank")
            .accountType(AccountType.PERSONAL)
            .currencyType(CurrencyType.KRW)
            .balance(balance)
            .accountStatus(AccountStatus.ACTIVE)
            .createdTimeStamp(TimeUtils.nowKst())
            .build();
    }

    /**
     * 특정 상태의 계좌
     */
    private AccountEntity createTestAccountWithStatus(String accountNumber, String accountName, BigDecimal balance, AccountStatus status) {
        return AccountEntity.builder()
            .accountNumber(accountNumber)
            .accountName(accountName)
            .bankName("mxxikrBank")
            .accountType(AccountType.PERSONAL)
            .currencyType(CurrencyType.KRW)
            .balance(balance)
            .accountStatus(status)
            .createdTimeStamp(TimeUtils.nowKst())
            .build();
    }

    /**
     * 특정 통화의 계좌
     */
    private AccountEntity createTestAccountWithCurrency(String accountNumber, String accountName, BigDecimal balance, CurrencyType currency) {
        return AccountEntity.builder()
                .accountNumber(accountNumber)
                .accountName(accountName)
                .bankName("mxxikrBank")
                .accountType(AccountType.PERSONAL)
                .currencyType(currency)
                .balance(balance)
                .accountStatus(AccountStatus.ACTIVE)
                .createdTimeStamp(TimeUtils.nowKst())
                .build();
    }

    /**
     * 계좌를 데이터베이스에 저장
     */
    private AccountEntity saveAccount(String accountNumber, String accountName, BigDecimal balance) {
        AccountEntity accountEntity = createTestAccount(accountNumber, accountName, balance);
        return entityManager.persistAndFlush(accountEntity);
    }

    // ==================== Repository 메서드 테스트 ====================
    @Nested
    class AccountRepositoryMethodTest {

        /**
         * 계좌 저장 성공
         */
        @Test
        void save_success() {
            AccountEntity accountEntity = createTestAccount(testFromAccountNumber, "mxxikr", new BigDecimal("100000"));

            AccountEntity savedAccount = accountRepository.save(accountEntity);

            assertThat(savedAccount).isNotNull();
            assertThat(savedAccount.getAccountId()).isNotNull();
            assertThat(savedAccount.getAccountNumber()).isEqualTo(testFromAccountNumber);
            assertThat(savedAccount.getAccountName()).isEqualTo("mxxikr");
            assertThat(savedAccount.getBalance()).isEqualByComparingTo(new BigDecimal("100000"));
        }

        /**
         * ID로 계좌 조회 성공
         */
        @Test
        void findById_success() {
            AccountEntity accountEntity = saveAccount(testFromAccountNumber, "mxxikr", new BigDecimal("100000"));
            UUID accountId = accountEntity.getAccountId();

            Optional<AccountEntity> foundAccount = accountRepository.findById(accountId);

            assertThat(foundAccount).isPresent();
            assertThat(foundAccount.get().getAccountNumber()).isEqualTo(testFromAccountNumber);
            assertThat(foundAccount.get().getAccountName()).isEqualTo("mxxikr");
        }

        /**
         * 존재하지 않는 ID로 조회 시 빈 Optional 반환
         */
        @Test
        void findById_notFound() {
            UUID nonExistentId = UUID.randomUUID();

            Optional<AccountEntity> foundAccount = accountRepository.findById(nonExistentId);

            assertThat(foundAccount).isEmpty();
        }

        /**
         * 계좌 삭제 성공 테스트
         */
        @Test
        void delete_success() {
            AccountEntity savedAccount = saveAccount(testFromAccountNumber, "mxxikr", new BigDecimal("100000"));
            UUID accountId = savedAccount.getAccountId();

            accountRepository.delete(savedAccount);
            entityManager.flush();

            Optional<AccountEntity> deletedAccount = accountRepository.findById(accountId);
            assertThat(deletedAccount).isEmpty();
        }

        /**
         * 여러 개의 계좌 전체 조회 테스트
         */
        @ParameterizedTest
        @MethodSource("multipleAccountsProvider")
        void findAll_multipleAccounts(int accountCount) {
            for (int i = 1; i <= accountCount; i++) {
                saveAccount(String.valueOf(001 + i * 1111111111), "test" + i, new BigDecimal("100000"));
            }

            List<AccountEntity> accounts = accountRepository.findAll();

            assertThat(accounts).hasSize(accountCount);
        }

        /**
         * 여러 계좌 개수 테스트 데이터 제공
         */
        private static Stream<Arguments> multipleAccountsProvider() {
            return Stream.of(
                Arguments.of(1),
                Arguments.of(3),
                Arguments.of(5),
                Arguments.of(10)
            );
        }
    }

    // ==================== 계좌 조회 테스트 ====================
    @Nested
    class FindByAccountNumberTest {

        /**
         * 계좌번호로 계좌 조회 성공
         */
        @Test
        void findByAccountNumber_success() {
            saveAccount(testFromAccountNumber, "mxxikr", new BigDecimal("100000"));

            Optional<AccountEntity> foundAccount = accountRepository.findByAccountNumber(testFromAccountNumber);

            assertThat(foundAccount).isPresent();
            assertThat(foundAccount.get().getAccountNumber()).isEqualTo(testFromAccountNumber);
            assertThat(foundAccount.get().getAccountName()).isEqualTo("mxxikr");
        }

        /**
         * 존재하지 않는 계좌 번호로 조회 시 빈 Optional 반환
         */
        @Test
        void findByAccountNumber_notFound() {
            Optional<AccountEntity> foundAccount = accountRepository.findByAccountNumber("nonexistent");

            assertThat(foundAccount).isEmpty();
        }
    }

    // ==================== 계좌번호 중복 확인 ====================
    @Nested
    class ExistsByAccountNumberTest {

        /**
         * 존재하는 계좌번호 중복 확인
         */
        @Test
        void existsByAccountNumber_exists() {
            saveAccount(testFromAccountNumber, "mxxikr", new BigDecimal("100000"));

            boolean exists = accountRepository.existsByAccountNumber(testFromAccountNumber);

            assertThat(exists).isTrue();
        }

        /**
         * 존재하지 않는 계좌번호 중복 확인
         */
        @Test
        void existsByAccountNumber_notExists() {
            boolean exists = accountRepository.existsByAccountNumber("nonexistent");

            assertThat(exists).isFalse();
        }

        /**
         * 다양한 시나리오의 계좌번호 중복 확인
         */
        @ParameterizedTest
        @MethodSource("existsTestDataProvider")
        void existsByAccountNumber_variousScenarios(String saveAccountNumber, String searchAccountNumber, boolean expectedExists) {
            if (saveAccountNumber != null) {
                saveAccount(saveAccountNumber, "mxxikr", new BigDecimal("100000"));
            }

            boolean exists = accountRepository.existsByAccountNumber(searchAccountNumber);

            assertThat(exists).isEqualTo(expectedExists);
        }

        /**
         * 중복 확인 테스트 데이터
         */
        private static Stream<Arguments> existsTestDataProvider() {
            return Stream.of(
                Arguments.of(testFromAccountNumber, testFromAccountNumber, true),
                Arguments.of(testFromAccountNumber, testToAccountNumber, false),
                Arguments.of(null, testFromAccountNumber, false),
                Arguments.of(testToAccountNumber, testFromAccountNumber, false),
                Arguments.of(testFromAccountNumber, testFromAccountNumber, true)
            );
        }
    }

    // ==================== 락을 사용한 계좌 조회 ====================
    @Nested
    class FindByAccountNumberLockTest {

        /**
         * 락을 사용한 계좌번호 조회 성공
         */
        @Test
        void findByAccountNumberLock_success() {
            saveAccount(testFromAccountNumber, "mxxikr", new BigDecimal("100000"));

            Optional<AccountEntity> foundAccount = accountRepository.findByAccountNumberLock(testFromAccountNumber);

            assertThat(foundAccount).isPresent();
            assertThat(foundAccount.get().getAccountNumber()).isEqualTo(testFromAccountNumber);
            assertThat(foundAccount.get().getBalance()).isEqualByComparingTo(new BigDecimal("100000"));
        }

        /**
         * 락을 사용한 존재하지 않는 계좌 조회
         */
        @Test
        void findByAccountNumberLock_notFound() {
            Optional<AccountEntity> foundAccount = accountRepository.findByAccountNumberLock("nonexistent");

            assertThat(foundAccount).isEmpty();
        }
    }

    // ==================== 계좌 잔액 업데이트 ====================
    @Nested
    class BalanceUpdateTest {

        /**
         * 계좌 잔액 업데이트 성공
         */
        @Test
        void updateBalance_success() {
            AccountEntity account = saveAccount(testFromAccountNumber, "mxxikr", new BigDecimal("100000"));
            BigDecimal newBalance = new BigDecimal("150000");

            account.updateBalance(newBalance);
            AccountEntity updatedAccount = accountRepository.save(account);

            assertThat(updatedAccount.getBalance()).isEqualByComparingTo(newBalance);

            Optional<AccountEntity> reloadedAccount = accountRepository.findById(account.getAccountId());
            assertThat(reloadedAccount).isPresent();
            assertThat(reloadedAccount.get().getBalance()).isEqualByComparingTo(newBalance);
        }

        /**
         * 다양한 금액의 잔액 업데이트
         */
        @ParameterizedTest
        @MethodSource("balanceUpdateProvider")
        void updateBalance_variousAmounts(BigDecimal initialBalance, BigDecimal updateAmount, BigDecimal expectedBalance) {
            AccountEntity account = saveAccount(testFromAccountNumber, "mxxikr", initialBalance);

            account.updateBalance(expectedBalance);
            accountRepository.save(account);

            Optional<AccountEntity> updatedAccount = accountRepository.findById(account.getAccountId());
            assertThat(updatedAccount).isPresent();
            assertThat(updatedAccount.get().getBalance()).isEqualByComparingTo(expectedBalance);
        }

        /**
         * 잔액 업데이트 테스트 데이터
         */
        private static Stream<Arguments> balanceUpdateProvider() {
            return Stream.of(
                Arguments.of(new BigDecimal("100000"), new BigDecimal("50000"), new BigDecimal("150000")),
                Arguments.of(new BigDecimal("200000"), new BigDecimal("-50000"), new BigDecimal("150000")),
                Arguments.of(new BigDecimal("0"), new BigDecimal("100000"), new BigDecimal("100000")),
                Arguments.of(new BigDecimal("1000"), new BigDecimal("0"), new BigDecimal("1000")),
                Arguments.of(new BigDecimal("100000.50"), new BigDecimal("50.25"), new BigDecimal("150000.75"))
            );
        }
    }

    // ==================== 계좌 상태별 조회====================
    @Nested
    class AccountStatusTest {

        /**
         * 다양한 계좌 상태로 저장 및 조회
         */
        @ParameterizedTest
        @MethodSource("accountStatusProvider")
        void saveAndRetrieve_differentStatuses(AccountStatus status) {
            AccountEntity account = createTestAccountWithStatus(testFromAccountNumber, "mxxikr", new BigDecimal("100000"), status);

            AccountEntity savedAccount = entityManager.persistAndFlush(account);

            Optional<AccountEntity> foundAccount = accountRepository.findById(savedAccount.getAccountId());
            assertThat(foundAccount).isPresent();
            assertThat(foundAccount.get().getAccountStatus()).isEqualTo(status);
        }

        /**
         * 계좌 상태 테스트 데이터
         */
        private static Stream<Arguments> accountStatusProvider() {
            return Stream.of(
                Arguments.of(AccountStatus.ACTIVE),
                Arguments.of(AccountStatus.INACTIVE),
                Arguments.of(AccountStatus.SUSPENDED)
            );
        }
    }

    // ==================== 통화 종류별 조회 ====================
    @Nested
    class CurrencyTypeTest {

        /**
         * 다양한 통화 종류로 저장 및 조회
         */
        @ParameterizedTest
        @MethodSource("currencyTypeProvider")
        void saveAndRetrieve_differentCurrencies(CurrencyType currency) {
            AccountEntity account = createTestAccountWithCurrency(testFromAccountNumber, "mxxikr", new BigDecimal("100000"), currency);

            AccountEntity savedAccount = entityManager.persistAndFlush(account);

            Optional<AccountEntity> foundAccount = accountRepository.findById(savedAccount.getAccountId());
            assertThat(foundAccount).isPresent();
            assertThat(foundAccount.get().getCurrencyType()).isEqualTo(currency);
        }

        /**
         * 통화 종류 테스트 데이터
         */
        private static Stream<Arguments> currencyTypeProvider() {
            return Stream.of(
                Arguments.of(CurrencyType.KRW),
                Arguments.of(CurrencyType.USD),
                Arguments.of(CurrencyType.EUR)
            );
        }
    }

    // ==================== 예외 상황 ====================
    @Nested
    class ExceptionTest {

        /**
         * 중복 계좌번호 저장 시 예외 발생
         */
        @Test
        void save_duplicateAccountNumber() {
            saveAccount(testFromAccountNumber, "mxxikr", new BigDecimal("100000"));

            AccountEntity duplicateAccount = createTestAccount(testFromAccountNumber, "mxxikr2", new BigDecimal("200000"));

            assertThatThrownBy(() -> {
                accountRepository.save(duplicateAccount);
                entityManager.flush();
            }).isInstanceOf(Exception.class);
        }

        /**
         * null 계좌번호 저장 시 예외 발생
         */
        @Test
        void save_nullAccountNumber() {
            AccountEntity accountWithNullNumber = AccountEntity.builder()
                .accountNumber(null)
                .accountName("mxxikr")
                .balance(new BigDecimal("100000"))
                .build();

            assertThatThrownBy(() -> {
                accountRepository.save(accountWithNullNumber);
                entityManager.flush();
            }).isInstanceOf(Exception.class);
        }

        /**
         * null 잔액 저장 시 예외 발생
         */
        @Test
        void save_nullBalance() {
            AccountEntity accountWithNullBalance = AccountEntity.builder()
                .accountNumber(testFromAccountNumber)
                .accountName("mxxikr")
                .balance(null)
                .build();

            assertThatThrownBy(() -> {
                accountRepository.save(accountWithNullBalance);
                entityManager.flush();
            }).isInstanceOf(Exception.class);
        }
    }

    // ==================== 경계 값 테스트 ====================
    @Nested
    class BoundaryValueTest {

        /**
         * 최소한의 유효한 계좌 저장
         */
        @Test
        void save_minimalValidAccount() {
            AccountEntity minimalAccount = AccountEntity.builder()
                .accountNumber(testFromAccountNumber)
                .balance(BigDecimal.ZERO)
                .build();

            AccountEntity savedAccount = accountRepository.save(minimalAccount);

            assertThat(savedAccount).isNotNull();
            assertThat(savedAccount.getAccountId()).isNotNull();
        }

        /**
         * 큰 금액 잔액 저장
         */
        @Test
        void save_largeBalance() {
            BigDecimal largeBalance = new BigDecimal("999999999999.99");
            AccountEntity account = createTestAccount(testFromAccountNumber, "mxxikr", largeBalance);

            AccountEntity savedAccount = accountRepository.save(account);

            assertThat(savedAccount.getBalance()).isEqualByComparingTo(largeBalance);
        }
    }

    // ==================== 락 메서드 테스트 ====================
    @Nested
    class ConcurrencyTest {
        /**
         * 락 메서드 기본 동작 검증
         */
        @Test
        void findByAccountNumberLock_concurrentAccess() {
            saveAccount(testFromAccountNumber, "mxxikr", new BigDecimal("100000"));

            Optional<AccountEntity> account1 = accountRepository.findByAccountNumberLock(testFromAccountNumber);
            Optional<AccountEntity> account2 = accountRepository.findByAccountNumberLock(testFromAccountNumber);

            assertThat(account1).isPresent();
            assertThat(account2).isPresent();
            assertThat(account1.get().getAccountId()).isEqualTo(account2.get().getAccountId());
        }
    }
}