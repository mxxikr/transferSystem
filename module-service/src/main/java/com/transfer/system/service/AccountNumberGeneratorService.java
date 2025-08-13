package com.transfer.system.service;

import com.transfer.system.domain.AccountNumberEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
public class AccountNumberGeneratorService {

    private final EntityManager entityManager;

    private static final String ACCOUNT_PREFIX = "001";

    @Transactional
    public String generateAccountNumber() {
        LocalDate today = LocalDate.now();

        // 날짜 기준으로 시퀀스 엔티티를 가져오거나 생성
        AccountNumberEntity seq = entityManager.find(AccountNumberEntity.class, today, LockModeType.PESSIMISTIC_WRITE);

        if (seq == null) {
            seq = new AccountNumberEntity();
            seq.setId(today);
            seq.setLastNumber(1L);
            entityManager.persist(seq);
        } else {
            seq.setLastNumber(seq.getLastNumber() + 1);
        }

        return format(today, seq.getLastNumber());
    }

    private String format(LocalDate date, Long value) {
        String datePart = date.format(DateTimeFormatter.ofPattern("yyMMdd")); // 6자리 날짜
        String sequencePart = String.format("%05d", value); // 5자리 시퀀스
        return ACCOUNT_PREFIX + datePart + sequencePart; // 001YYMMDD00001
    }
}