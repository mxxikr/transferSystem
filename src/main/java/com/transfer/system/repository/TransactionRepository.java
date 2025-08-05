package com.transfer.system.repository;

import com.transfer.system.domain.AccountEntity;
import com.transfer.system.domain.TransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface TransactionRepository extends JpaRepository<TransactionEntity, UUID> {
    @Query("""
       SELECT te FROM TransactionEntity te
       WHERE te.fromAccount = :account OR te.toAccount = :account
       ORDER BY te.createdTimeStamp DESC
    """)
    List<TransactionEntity> findAllByAccount(@Param("account") AccountEntity account); // 특정 계좌의 모든 거래 내역 조회
}