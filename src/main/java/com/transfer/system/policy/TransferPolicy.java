package com.transfer.system.policy;

import com.transfer.system.exception.ErrorCode;
import com.transfer.system.exception.TransferSystemException;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
@Component
public class TransferPolicy {

    private final BigDecimal feeRate; // 수수료율
    private final BigDecimal withdrawDailyLimit; // 출금 일일 한도
    private final BigDecimal transferDailyLimit; // 이체 일일 한도

    public TransferPolicy(
        @Value("${transfer.fee-rate}") BigDecimal feeRate,
        @Value("${transfer.withdraw-daily-limit}") BigDecimal withdrawDailyLimit,
        @Value("${transfer.transfer-daily-limit}") BigDecimal transferDailyLimit) {

        this.feeRate = feeRate;
        this.withdrawDailyLimit = withdrawDailyLimit;
        this.transferDailyLimit = transferDailyLimit;
    }

    public BigDecimal calculateFee(BigDecimal amount) {
        return amount.multiply(feeRate).setScale(2, RoundingMode.HALF_UP);
    }

    public void validateWithdrawAmount(BigDecimal amount) {
        if (amount.compareTo(withdrawDailyLimit) > 0) {
            throw new TransferSystemException(ErrorCode.EXCEEDS_WITHDRAW_LIMIT);
        }
    }
}