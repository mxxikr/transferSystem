package com.transfer.system.repository;

import com.transfer.system.domain.AccountEntity;
import com.transfer.system.domain.TransactionEntity;
import com.transfer.system.enums.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {
    @Query("""
        SELECT te
        FROM TransactionEntity te
        WHERE te.fromAccount = :account
           OR te.toAccount = :account
        ORDER BY te.createdTimeStamp DESC
    """)
    Page<TransactionEntity> findAllByAccount(@Param("account") AccountEntity account, Pageable pageable); // 특정 계좌의 모든 거래 내역 조회

    // 일일 한도 계산용 조회
    @Query("""
        SELECT COALESCE(SUM(t.amount), 0)
        FROM TransactionEntity t
        WHERE t.fromAccount.accountNumber = :accountNumber
          AND t.transactionType = :type
          AND t.createdTimeStamp BETWEEN :startTime AND :endTime
    """)
    BigDecimal getSumTodayUsedAmount(@Param("accountNumber") String accountNumber, @Param("type") TransactionType type, @Param("startTime") LocalDateTime startTime, @Param("endTime") LocalDateTime endTime);
}