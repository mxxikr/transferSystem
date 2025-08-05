package com.transfer.system.repository;

import com.transfer.system.domain.AccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<AccountEntity, UUID> {
    Optional<AccountEntity> findByAccountNumber(String accountNumber); // 계좌 번호로 계좌 조회
    boolean existsByAccountNumber(String accountNumber); // 계좌 번호로 계좌 존재 여부 확인
}