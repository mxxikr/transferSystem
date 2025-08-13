package com.transfer.system.domain;

import com.transfer.system.enums.TransactionType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.GenericGenerator;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "transaction_entity")
public class TransactionEntity {
    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(columnDefinition = "BINARY(16)")
    private UUID transactionId; // 거래 고유 식별자

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_account_id")
    private AccountEntity fromAccount; // 출금 계좌

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_account_id")
    private AccountEntity toAccount; // 입금 계좌

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull
    private TransactionType transactionType; // 거래 유형

    @Column(nullable = false)
    private BigDecimal amount; // 전송 금액
    private BigDecimal fee; // 수수료

    private LocalDateTime createdTimeStamp; // 거래 생성 일시
}
