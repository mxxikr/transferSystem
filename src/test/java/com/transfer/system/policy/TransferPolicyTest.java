package com.transfer.system.policy;

import com.transfer.system.exception.ErrorCode;
import com.transfer.system.exception.TransferSystemException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TransferPolicyTest {

    private TransferPolicy transferPolicy;

    private final BigDecimal TEST_FEE_RATE = new BigDecimal("0.01");
    private final BigDecimal TEST_WITHDRAW_DAILY_LIMIT = new BigDecimal("1000000");
    private final BigDecimal TEST_TRANSFER_DAILY_LIMIT = new BigDecimal("3000000");

    @BeforeEach
    void setUp() {
        transferPolicy = new TransferPolicy(
                TEST_FEE_RATE,
                TEST_WITHDRAW_DAILY_LIMIT,
                TEST_TRANSFER_DAILY_LIMIT
        );
    }

    @Nested
    class CalculateFeeTest {

        @Test
        void calculateFee_success() {
            BigDecimal amount = new BigDecimal("100000");

            BigDecimal fee = transferPolicy.calculateFee(amount);

            BigDecimal expectedFee = new BigDecimal("1000.00");
            assertEquals(expectedFee, fee);
        }

        @ParameterizedTest
        @MethodSource("feeCalculationProvider")
        void calculateFee_success_variousAmounts(BigDecimal amount, BigDecimal expectedFee) {
            BigDecimal actualFee = transferPolicy.calculateFee(amount);

            assertEquals(expectedFee, actualFee);
        }

        private static Stream<Arguments> feeCalculationProvider() {
            return Stream.of(
                    Arguments.of(new BigDecimal("50000"), new BigDecimal("500.00")),
                    Arguments.of(new BigDecimal("200000"), new BigDecimal("2000.00")),
                    Arguments.of(new BigDecimal("1000000"), new BigDecimal("10000.00")),
                    Arguments.of(new BigDecimal("1"), new BigDecimal("0.01")),
                    Arguments.of(new BigDecimal("100"), new BigDecimal("1.00")),
                    Arguments.of(new BigDecimal("12345"), new BigDecimal("123.45"))
            );
        }

        @Test
        void calculateFee_success_roundingMode() {
            BigDecimal amount = new BigDecimal("12345.67");

            BigDecimal fee = transferPolicy.calculateFee(amount);

            BigDecimal expectedFee = new BigDecimal("123.46");
            assertEquals(expectedFee, fee);
            assertEquals(2, fee.scale());
        }

        @ParameterizedTest
        @ValueSource(strings = {"1", "50", "99", "100", "150"})
        void calculateFee_success_smallAmounts(String amountStr) {
            BigDecimal amount = new BigDecimal(amountStr);

            BigDecimal fee = transferPolicy.calculateFee(amount);

            assertTrue(fee.compareTo(BigDecimal.ZERO) >= 0);
            assertEquals(2, fee.scale());
        }

        @Test
        void calculateFee_success_largeAmount() {
            BigDecimal largeAmount = new BigDecimal("10000000");

            BigDecimal fee = transferPolicy.calculateFee(largeAmount);

            BigDecimal expectedFee = new BigDecimal("100000.00");
            assertEquals(expectedFee, fee);
        }

        @Test
        void calculateFee_success_extremeValues() {
            BigDecimal minAmount = new BigDecimal("0.01");
            BigDecimal minFee = transferPolicy.calculateFee(minAmount);
            assertEquals(new BigDecimal("0.00"), minFee);

            BigDecimal maxAmount = new BigDecimal("999999999.99");
            BigDecimal maxFee = transferPolicy.calculateFee(maxAmount);
            assertTrue(maxFee.compareTo(BigDecimal.ZERO) > 0);
        }
    }

    @Nested
    class ValidateWithdrawAmountTest {

        @Test
        void validateWithdrawAmount_success() {
            String accountNumber = "test-account-123";
            BigDecimal amount = new BigDecimal("500000");
            BigDecimal todayWithdrawTotal = new BigDecimal("300000");

            assertDoesNotThrow(() ->
                    transferPolicy.validateWithdrawAmount(accountNumber, amount, todayWithdrawTotal)
            );
        }

        @Test
        void validateWithdrawAmount_success_exactLimit() {
            String accountNumber = "test-account-123";
            BigDecimal amount = new BigDecimal("400000");
            BigDecimal todayWithdrawTotal = new BigDecimal("600000");

            assertDoesNotThrow(() ->
                    transferPolicy.validateWithdrawAmount(accountNumber, amount, todayWithdrawTotal)
            );
        }

        @Test
        void validateWithdrawAmount_fail_whenExceedsLimit() {
            String accountNumber = "test-account-123";
            BigDecimal amount = new BigDecimal("500000");
            BigDecimal todayWithdrawTotal = new BigDecimal("600000");

            TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
                    transferPolicy.validateWithdrawAmount(accountNumber, amount, todayWithdrawTotal)
            );

            assertEquals(ErrorCode.EXCEEDS_WITHDRAW_LIMIT, exception.getErrorCode());
        }

        @Test
        void validateWithdrawAmount_fail_whenTodayTotalExceedsLimit() {
            String accountNumber = "test-account-123";
            BigDecimal amount = new BigDecimal("1");
            BigDecimal todayWithdrawTotal = TEST_WITHDRAW_DAILY_LIMIT;

            TransferSystemException exception = assertThrows(TransferSystemException.class, () ->
                    transferPolicy.validateWithdrawAmount(accountNumber, amount, todayWithdrawTotal)
            );

            assertEquals(ErrorCode.EXCEEDS_WITHDRAW_LIMIT, exception.getErrorCode());
        }

        @ParameterizedTest
        @MethodSource("withdrawScenarioProvider")
        void validateWithdrawAmount_variousScenarios(BigDecimal amount, BigDecimal todayUsed, boolean shouldPass) {
            String accountNumber = "test-account-123";

            if (shouldPass) {
                assertDoesNotThrow(() ->
                        transferPolicy.validateWithdrawAmount(accountNumber, amount, todayUsed)
                );
            } else {
                assertThrows(TransferSystemException.class, () ->
                        transferPolicy.validateWithdrawAmount(accountNumber, amount, todayUsed)
                );
            }
        }

        private static Stream<Arguments> withdrawScenarioProvider() {
            return Stream.of(
                    Arguments.of(new BigDecimal("100000"), BigDecimal.ZERO, true),
                    Arguments.of(new BigDecimal("1000000"), BigDecimal.ZERO, true),
                    Arguments.of(new BigDecimal("1000001"), BigDecimal.ZERO, false),
                    Arguments.of(new BigDecimal("500000"), new BigDecimal("500000"), true),
                    Arguments.of(new BigDecimal("500000"), new BigDecimal("500001"), false),
                    Arguments.of(new BigDecimal("1"), new BigDecimal("999999"), true),
                    Arguments.of(new BigDecimal("2"), new BigDecimal("999999"), false)
            );
        }

        @Test
        void validateWithdrawAmount_boundaryValues() {
            String accountNumber = "test-account-123";

            BigDecimal almostLimit = TEST_WITHDRAW_DAILY_LIMIT.subtract(BigDecimal.ONE);
            assertDoesNotThrow(() ->
                    transferPolicy.validateWithdrawAmount(accountNumber, almostLimit, BigDecimal.ZERO)
            );

            assertDoesNotThrow(() ->
                    transferPolicy.validateWithdrawAmount(accountNumber, TEST_WITHDRAW_DAILY_LIMIT, BigDecimal.ZERO)
            );

            BigDecimal overLimit = TEST_WITHDRAW_DAILY_LIMIT.add(BigDecimal.ONE);
            assertThrows(TransferSystemException.class, () ->
                    transferPolicy.validateWithdrawAmount(accountNumber, overLimit, BigDecimal.ZERO)
            );
        }
    }

    @Nested
    class PolicyConfigurationTest {

        @Test
        void getFeeRate_configuration() {
            BigDecimal feeRate = transferPolicy.getFeeRate();

            assertEquals(TEST_FEE_RATE, feeRate);
            assertTrue(feeRate.compareTo(BigDecimal.ZERO) > 0);
            assertTrue(feeRate.compareTo(BigDecimal.ONE) < 0);
        }

        @Test
        void getWithdrawDailyLimit_configuration() {
            BigDecimal withdrawLimit = transferPolicy.getWithdrawDailyLimit();

            assertEquals(TEST_WITHDRAW_DAILY_LIMIT, withdrawLimit);
            assertTrue(withdrawLimit.compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        void getTransferDailyLimit_configuration() {
            BigDecimal transferLimit = transferPolicy.getTransferDailyLimit();

            assertEquals(TEST_TRANSFER_DAILY_LIMIT, transferLimit);
            assertTrue(transferLimit.compareTo(BigDecimal.ZERO) > 0);
        }

        @Test
        void policyConfiguration_consistency() {
            BigDecimal feeRate = transferPolicy.getFeeRate();
            BigDecimal withdrawLimit = transferPolicy.getWithdrawDailyLimit();
            BigDecimal transferLimit = transferPolicy.getTransferDailyLimit();

            assertTrue(feeRate.compareTo(BigDecimal.ZERO) > 0);
            assertTrue(withdrawLimit.compareTo(BigDecimal.ZERO) > 0);
            assertTrue(transferLimit.compareTo(BigDecimal.ZERO) > 0);
            assertTrue(transferLimit.compareTo(withdrawLimit) >= 0);
            assertTrue(feeRate.compareTo(new BigDecimal("0.1")) <= 0);
        }
    }

    @Nested
    class BoundaryAndExceptionTest {

        @Test
        void calculateFee_zeroAmount() {
            BigDecimal zeroAmount = BigDecimal.ZERO;

            BigDecimal fee = transferPolicy.calculateFee(zeroAmount);

            assertEquals(new BigDecimal("0.00"), fee);
        }

        @Test
        void calculateFee_verySmallAmount() {
            BigDecimal smallAmount = new BigDecimal("0.01");

            BigDecimal fee = transferPolicy.calculateFee(smallAmount);

            assertTrue(fee.compareTo(BigDecimal.ZERO) >= 0);
            assertEquals(2, fee.scale());
        }

        @Test
        void validateWithdrawAmount_zeroAmount() {
            String accountNumber = "test-account-123";
            BigDecimal zeroAmount = BigDecimal.ZERO;
            BigDecimal todayUsed = BigDecimal.ZERO;

            assertDoesNotThrow(() ->
                    transferPolicy.validateWithdrawAmount(accountNumber, zeroAmount, todayUsed)
            );
        }

        @Test
        void validateWithdrawAmount_zeroTodayUsed() {
            String accountNumber = "test-account-123";
            BigDecimal amount = new BigDecimal("100000");
            BigDecimal zeroTodayUsed = BigDecimal.ZERO;

            assertDoesNotThrow(() ->
                    transferPolicy.validateWithdrawAmount(accountNumber, amount, zeroTodayUsed)
            );
        }

        @Test
        void extremeValues_handling() {
            String accountNumber = "test-account-123";

            BigDecimal hugeAmount = new BigDecimal("999999999999.99");
            BigDecimal hugeFee = transferPolicy.calculateFee(hugeAmount);
            assertTrue(hugeFee.compareTo(BigDecimal.ZERO) > 0);
            assertEquals(2, hugeFee.scale());

            BigDecimal tinyAmount = new BigDecimal("0.01");
            BigDecimal tinyFee = transferPolicy.calculateFee(tinyAmount);
            assertTrue(tinyFee.compareTo(BigDecimal.ZERO) >= 0);

            assertThrows(TransferSystemException.class, () ->
                    transferPolicy.validateWithdrawAmount(accountNumber, hugeAmount, BigDecimal.ZERO)
            );
        }
    }

    @Nested
    class RealWorldScenarioTest {

        @Test
        void scenario_normalSmallTransfer() {
            String accountNumber = "account-001";
            BigDecimal amount = new BigDecimal("50000");
            BigDecimal todayUsed = BigDecimal.ZERO;

            BigDecimal fee = transferPolicy.calculateFee(amount);

            assertEquals(new BigDecimal("500.00"), fee);
            assertDoesNotThrow(() ->
                    transferPolicy.validateWithdrawAmount(accountNumber, amount, todayUsed)
            );
        }

        @Test
        void scenario_largeTransfer() {
            String accountNumber = "account-002";
            BigDecimal amount = new BigDecimal("900000");
            BigDecimal todayUsed = new BigDecimal("50000");

            BigDecimal fee = transferPolicy.calculateFee(amount);

            assertEquals(new BigDecimal("9000.00"), fee);
            assertDoesNotThrow(() ->
                    transferPolicy.validateWithdrawAmount(accountNumber, amount, todayUsed)
            );
        }

        @Test
        void scenario_nearLimitTransfer() {
            String accountNumber = "account-003";
            BigDecimal amount = new BigDecimal("100000");
            BigDecimal todayUsed = new BigDecimal("890000");

            BigDecimal fee = transferPolicy.calculateFee(amount);

            assertEquals(new BigDecimal("1000.00"), fee);
            assertDoesNotThrow(() ->
                    transferPolicy.validateWithdrawAmount(accountNumber, amount, todayUsed)
            );
        }

        @Test
        void scenario_exceedLimitTransfer() {
            String accountNumber = "account-004";
            BigDecimal amount = new BigDecimal("200000");
            BigDecimal todayUsed = new BigDecimal("900000");

            BigDecimal fee = transferPolicy.calculateFee(amount);

            assertEquals(new BigDecimal("2000.00"), fee);
            assertThrows(TransferSystemException.class, () ->
                    transferPolicy.validateWithdrawAmount(accountNumber, amount, todayUsed)
            );
        }

        @Test
        void scenario_multipleTransfersPerDay() {
            String accountNumber = "account-005";

            BigDecimal amount1 = new BigDecimal("300000");
            BigDecimal fee1 = transferPolicy.calculateFee(amount1);
            assertEquals(new BigDecimal("3000.00"), fee1);

            BigDecimal firstTransferUsed = BigDecimal.ZERO;
            assertDoesNotThrow(() -> {
                transferPolicy.validateWithdrawAmount(accountNumber, amount1, firstTransferUsed);
            });

            BigDecimal amount2 = new BigDecimal("400000");
            BigDecimal fee2 = transferPolicy.calculateFee(amount2);
            assertEquals(new BigDecimal("4000.00"), fee2);

            BigDecimal secondTransferUsed = amount1;
            assertDoesNotThrow(() -> {
                transferPolicy.validateWithdrawAmount(accountNumber, amount2, secondTransferUsed);
            });

            BigDecimal amount3 = new BigDecimal("300000");
            BigDecimal fee3 = transferPolicy.calculateFee(amount3);
            assertEquals(new BigDecimal("3000.00"), fee3);

            BigDecimal thirdTransferUsed = amount1.add(amount2);
            assertDoesNotThrow(() -> {
                transferPolicy.validateWithdrawAmount(accountNumber, amount3, thirdTransferUsed);
            });

            BigDecimal amount4 = BigDecimal.ONE;
            BigDecimal totalAfterThreeTransfers = amount1.add(amount2).add(amount3);

            assertThrows(TransferSystemException.class, () -> {
                transferPolicy.validateWithdrawAmount(accountNumber, amount4, totalAfterThreeTransfers);
            });
        }

        @Test
        void scenario_consecutiveTransfers() {
            String accountNumber = "account-006";

            validateSingleTransfer(accountNumber, new BigDecimal("200000"), BigDecimal.ZERO, true);
            validateSingleTransfer(accountNumber, new BigDecimal("300000"), new BigDecimal("200000"), true);
            validateSingleTransfer(accountNumber, new BigDecimal("400000"), new BigDecimal("500000"), true);
            validateSingleTransfer(accountNumber, new BigDecimal("100000"), new BigDecimal("900000"), true);
            validateSingleTransfer(accountNumber, new BigDecimal("100001"), new BigDecimal("900000"), false);
        }

        private void validateSingleTransfer(String accountNumber, BigDecimal amount,
                                            BigDecimal currentUsed, boolean shouldSucceed) {
            if (shouldSucceed) {
                assertDoesNotThrow(() ->
                        transferPolicy.validateWithdrawAmount(accountNumber, amount, currentUsed)
                );
            } else {
                assertThrows(TransferSystemException.class, () ->
                        transferPolicy.validateWithdrawAmount(accountNumber, amount, currentUsed)
                );
            }
        }
    }

    @Nested
    class IntegrationTest {

        @Test
        void integration_feeCalculationAndLimitValidation() {
            String accountNumber = "integration-test-001";
            BigDecimal amount = new BigDecimal("500000");
            BigDecimal todayUsed = new BigDecimal("450000");

            BigDecimal fee = transferPolicy.calculateFee(amount);

            assertEquals(new BigDecimal("5000.00"), fee);

            assertDoesNotThrow(() ->
                    transferPolicy.validateWithdrawAmount(accountNumber, amount, todayUsed)
            );

            assertTrue(transferPolicy.getFeeRate().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(transferPolicy.getWithdrawDailyLimit().compareTo(BigDecimal.ZERO) > 0);
            assertTrue(transferPolicy.getTransferDailyLimit().compareTo(BigDecimal.ZERO) > 0);
        }

        @ParameterizedTest
        @MethodSource("integrationScenarioProvider")
        void integration_variousScenarios(BigDecimal amount, BigDecimal todayUsed, boolean shouldPassValidation) {
            String accountNumber = "integration-test";

            BigDecimal fee = transferPolicy.calculateFee(amount);

            assertTrue(fee.compareTo(BigDecimal.ZERO) >= 0);
            assertEquals(2, fee.scale());

            if (shouldPassValidation) {
                assertDoesNotThrow(() ->
                        transferPolicy.validateWithdrawAmount(accountNumber, amount, todayUsed)
                );
            } else {
                assertThrows(TransferSystemException.class, () ->
                        transferPolicy.validateWithdrawAmount(accountNumber, amount, todayUsed)
                );
            }
        }

        private static Stream<Arguments> integrationScenarioProvider() {
            return Stream.of(
                    Arguments.of(new BigDecimal("100000"), BigDecimal.ZERO, true),
                    Arguments.of(new BigDecimal("1000000"), BigDecimal.ZERO, true),
                    Arguments.of(new BigDecimal("1000001"), BigDecimal.ZERO, false),
                    Arguments.of(new BigDecimal("500000"), new BigDecimal("500000"), true),
                    Arguments.of(new BigDecimal("500000"), new BigDecimal("500001"), false),
                    Arguments.of(BigDecimal.ONE, new BigDecimal("999999"), true),
                    Arguments.of(new BigDecimal("0.01"), BigDecimal.ZERO, true)
            );
        }
    }
}