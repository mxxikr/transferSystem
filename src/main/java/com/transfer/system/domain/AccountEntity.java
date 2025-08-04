package com.transfer.system.domain;

import com.transfer.system.enums.AccountStatus;
import com.transfer.system.enums.AccountType;
import com.transfer.system.enums.CurrencyType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class AccountEntity {
    @Id
    private Long accountId; // 계좌 고유 식별자
    @Column(nullable = false)
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

    @CreatedDate
    @Column(updatable = false)
    private LocalDateTime createdTimeStamp; // 계좌 생성 일시
    @UpdateTimestamp
    private LocalDateTime updatedTimeStamp; // 계좌 정보 수정 일시
}