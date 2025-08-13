package com.transfer.system.service;

import com.transfer.system.domain.AccountEntity;
import com.transfer.system.domain.TransactionEntity;
import com.transfer.system.dto.TransactionRequestDTO;
import com.transfer.system.dto.TransactionResponseDTO;
import com.transfer.system.enums.AccountStatus;
import com.transfer.system.enums.TransactionType;
import com.transfer.system.exception.ErrorCode;
import com.transfer.system.exception.TransferSystemException;
import com.transfer.system.policy.PagingPolicy;
import com.transfer.system.policy.TransferPolicy;
import com.transfer.system.repository.AccountRepository;
import com.transfer.system.repository.TransactionRepository;
import com.transfer.system.utils.TimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransferPolicy transferPolicy;
    private final PagingPolicy pagingPolicy;

    /**
     * 이체 기능
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TransactionResponseDTO transfer(TransactionRequestDTO transactionRequestDTO) {
        if (transactionRequestDTO == null) {
            throw new TransferSystemException(ErrorCode.INVALID_REQUEST);
        }

        String fromAccountNumber = transactionRequestDTO.getFromAccountNumber();
        String toAccountNumber = transactionRequestDTO.getToAccountNumber();
        BigDecimal amount = transactionRequestDTO.getAmount();

        if (fromAccountNumber == null || toAccountNumber == null) {
            throw new TransferSystemException(ErrorCode.INVALID_ACCOUNT_NUMBER);
        }

        if (fromAccountNumber.equals(toAccountNumber)) { // 같은 계좌로는 이체할 수 없음
            throw new TransferSystemException(ErrorCode.TRANSFER_SAME_ACCOUNT);
        }

        // 이체 금액 유효성 검사
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransferSystemException(ErrorCode.INVALID_AMOUNT);
        }

        log.info("[TransactionService] From: {}, To: {}, Amount: {}", fromAccountNumber, toAccountNumber, amount);

        // 락 순서 고정
        AccountEntity firstLock, secondLock;

        if (fromAccountNumber.compareTo(toAccountNumber) < 0) {
            firstLock = accountRepository.findByAccountNumberLock(fromAccountNumber)
                .orElseThrow(() -> new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));
            secondLock = accountRepository.findByAccountNumberLock(toAccountNumber)
                .orElseThrow(() -> new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));
        } else {
            firstLock = accountRepository.findByAccountNumberLock(toAccountNumber)
                .orElseThrow(() -> new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));
            secondLock = accountRepository.findByAccountNumberLock(fromAccountNumber)
                .orElseThrow(() -> new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));
        }
        
        AccountEntity fromAccount = fromAccountNumber.equals(firstLock.getAccountNumber()) ? firstLock : secondLock;
        AccountEntity toAccount = getAccountEntity(fromAccount, firstLock, secondLock);

        // 이체 수수료 유효성 검사
        BigDecimal fee = transferPolicy.calculateFee(amount); // 이체 수수료 계산

        if (fee == null || fee.compareTo(BigDecimal.ZERO) < 0) {
            throw new TransferSystemException(ErrorCode.INVALID_FEE);
        }

        BigDecimal total = amount.add(fee); // 총 이쳬 금액


        log.info("[TransactionService] 수수료 계산 결과 Amount : {}, Fee : {}, Total : {}", amount, fee, total);

        // 이체 한도 확인
        LocalDateTime startTime = TimeUtils.startOfTodayKst();
        LocalDateTime endTime = TimeUtils.endOfTodayKst();

        BigDecimal todayUsed = transactionRepository.getSumTodayUsedAmount(fromAccountNumber, TransactionType.TRANSFER, startTime, endTime);
        todayUsed = todayUsed != null ? todayUsed : BigDecimal.ZERO;

        transferPolicy.validateTransferAmount(amount, todayUsed);

        // 잔액 확인
        if (fromAccount.getBalance().compareTo(total) < 0) { // 출금 계좌의 잔액이 이체 금액보다 많아야 함
            log.warn("[TransactionService] 잔액 부족 Account : {}, 이체 금액 : {}, 총 잔액 : {}", fromAccountNumber, total, fromAccount.getBalance());
            throw new TransferSystemException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        // 계좌 잔액 업데이트
        fromAccount.updateBalance(fromAccount.getBalance().subtract(total));
        toAccount.updateBalance(toAccount.getBalance().add(amount));

        // 기록 저장
        TransactionEntity transactionEntity = TransactionEntity.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .transactionType(TransactionType.TRANSFER)
                .amount(amount)
                .fee(fee)
                .createdTimeStamp(TimeUtils.nowKst())
                .build();

        TransactionEntity savedTransactionEntity = transactionRepository.save(transactionEntity);
        log.info("[TranscationService] 이체 완료 거래ID : {}", savedTransactionEntity.getTransactionId());

        return TransactionResponseDTO.from(savedTransactionEntity);
    }

    private static AccountEntity getAccountEntity(AccountEntity fromAccount, AccountEntity firstLock, AccountEntity secondLock) {
        AccountEntity toAccount = fromAccount == firstLock ? secondLock : firstLock;

        // 수신 계좌 상태 확인
        if (toAccount.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new TransferSystemException(ErrorCode.RECEIVER_ACCOUNT_INACTIVE);
        }

        // 송신 계좌 상태 확인
        if (fromAccount.getAccountStatus() != AccountStatus.ACTIVE) {
            throw new TransferSystemException(ErrorCode.SENDER_ACCOUNT_INACTIVE);
        }

        // 통화 종류 일치 확인
        if (fromAccount.getCurrencyType() == null || toAccount.getCurrencyType() == null || !fromAccount.getCurrencyType().equals(toAccount.getCurrencyType())) {
            throw new TransferSystemException(ErrorCode.CURRENCY_TYPE_MISMATCH);
        }
        return toAccount;
    }

    /**
     * 계좌 거래 내역 조회
     */
    @Override
    @Transactional(readOnly = true)
    public Page<TransactionResponseDTO> getTransactionHistory(String accountNumber, int page, int size) {
        // 계좌번호 검증
        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new TransferSystemException(ErrorCode.INVALID_ACCOUNT_NUMBER);
        }

        // 계좌 존재 여부 확인
        AccountEntity account = accountRepository.findByAccountNumber(accountNumber)
            .orElseThrow(() -> new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));

        // 페이징 정책 적용
        int validatedPage = pagingPolicy.getValidatedPage(page >= 0 ? page : null);
        int validatedSize = pagingPolicy.getValidatedSize(size);

        Pageable pageable = PageRequest.of(
            validatedPage,
            validatedSize,
            Sort.by(pagingPolicy.getTransactionSortField()).descending()
        );

        Page<TransactionEntity> transactions = transactionRepository.findAllByAccount(account, pageable);

        log.info("[TranscationService] 거래 내역 조회 완료: 총 {}건, 현재 페이지 {}건", transactions.getTotalElements(), transactions.getNumberOfElements());

        return transactions.map(TransactionResponseDTO::from);
    }
}