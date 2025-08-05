package com.transfer.system.domain;

import com.transfer.system.enums.AccountStatus;
import com.transfer.system.enums.AccountType;
import com.transfer.system.enums.CurrencyType;
import com.transfer.system.exception.ErrorCode;
import com.transfer.system.exception.TransferSystemException;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountEntity {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    private UUID accountId; // 계좌 고유 식별자

    @Column(nullable = false, unique = true)
    private String accountNumber; // 계좌 번호

    private String accountName; // 계좌 이름

    private String bankName; // 은행 이름

    @Enumerated(EnumType.STRING)
    private AccountType accountType; // 계좌 유형

    private CurrencyType currencyType; // 통화 종류

    @Column(nullable = false)
    private BigDecimal balance; // 계좌 잔액

    @Enumerated(EnumType.STRING)
    private AccountStatus accountStatus; // 계좌 상태

    @Column(updatable = false)
    private LocalDateTime createdTimeStamp; // 계좌 생성 일시

    private LocalDateTime updatedTimeStamp; // 계좌 정보 수정 일시

    // 출금
    public void subtractBalance(BigDecimal amount) {
        if (this.balance.compareTo(amount) < 0) { // 잔액 부족 여부 확인
            throw new TransferSystemException(ErrorCode.INSUFFICIENT_BALANCE);
        }
        this.balance = this.balance.subtract(amount);
        this.updatedTimeStamp = LocalDateTime.now();
    }

    // 입금
    public void addBalance(BigDecimal amount) {
        this.balance = this.balance.add(amount);
        this.updatedTimeStamp = LocalDateTime.now();
    }

    // 계좌 잔액 업데이트
    public void updateBalance(BigDecimal newBalance) {
        if (newBalance.compareTo(BigDecimal.ZERO) < 0) {
            throw new TransferSystemException(ErrorCode.NEGATIVE_BALANCE_NOT_ALLOWED);
        }
        this.balance = newBalance;
        this.updatedTimeStamp = LocalDateTime.now();
    }
}