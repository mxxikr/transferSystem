package com.transfer.system.service;

import com.transfer.system.domain.AccountEntity;
import com.transfer.system.domain.TransactionEntity;
import com.transfer.system.dto.AccountBalanceResponseDTO;
import com.transfer.system.dto.AccountCreateRequestDTO;
import com.transfer.system.dto.AccountResponseDTO;
import com.transfer.system.enums.AccountStatus;
import com.transfer.system.exception.ErrorCode;
import com.transfer.system.exception.TransferSystemException;
import com.transfer.system.policy.TransferPolicy;
import com.transfer.system.repository.AccountRepository;
import com.transfer.system.repository.TransactionRepository;
import com.transfer.system.enums.TransactionType;
import com.transfer.system.utils.MoneyUtils;
import com.transfer.system.utils.TimeUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final TransferPolicy transferPolicy;
    private final TransactionRepository transactionRepository;
    private final AccountNumberGeneratorService accountNumberGeneratorService;

    private static final String BANK_NAME = "mxxikrBank";

    /***
     * 계좌 생성
     */
    @Override
    @Transactional
    public AccountResponseDTO createAccount(AccountCreateRequestDTO accountCreateRequestDTO) {
        if (accountCreateRequestDTO == null) {
            throw new TransferSystemException(ErrorCode.INVALID_REQUEST);
        }

        if (accountCreateRequestDTO.getAccountName() == null || accountCreateRequestDTO.getAccountType() == null || accountCreateRequestDTO.getCurrencyType() == null) {
            throw new TransferSystemException(ErrorCode.INVALID_REQUEST);
        }

        String accountNumber = accountNumberGeneratorService.generateAccountNumber();

        if (accountNumber == null || accountNumber.trim().isEmpty()) {
            throw new TransferSystemException(ErrorCode.INVALID_REQUEST);
        }

        if (accountRepository.existsByAccountNumber(accountNumber)) {
            throw new TransferSystemException(ErrorCode.DUPLICATE_ACCOUNT_NUMBER);
        }

        AccountEntity accountEntity = AccountEntity.builder()
            .accountNumber(accountNumber)
            .accountName(accountCreateRequestDTO.getAccountName())
            .bankName(BANK_NAME)
            .accountType(accountCreateRequestDTO.getAccountType())
            .currencyType(accountCreateRequestDTO.getCurrencyType())
            .balance(MoneyUtils.normalize(BigDecimal.ZERO))
            .accountStatus(AccountStatus.ACTIVE)
            .createdTimeStamp(TimeUtils.nowKstLocalDateTime())
            .updatedTimeStamp(TimeUtils.nowKstLocalDateTime())
            .build();

        return toDto(accountRepository.save(accountEntity));
    }

    /**
     * 계좌 조회
     */
    @Override
    public AccountResponseDTO getAccount(UUID id) {
        AccountEntity accountEntity = accountRepository.findById(id)
            .orElseThrow(() -> new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));
        return toDto(accountEntity);
    }

    /**
     * 계좌 삭제
     */
    @Override
    public void deleteAccount(UUID id) {
        AccountEntity accountEntity = accountRepository.findById(id)
            .orElseThrow(() -> new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));

        boolean hasTransaction = transactionRepository.existsByFromOrTo(accountEntity);

        // 거래 내역이 있고 계좌 상태가 ACTIVE인 경우 삭제 불가
        if (hasTransaction && accountEntity.getAccountStatus() == AccountStatus.ACTIVE) {
            throw new TransferSystemException(ErrorCode.ACCOUNT_HAS_TRANSACTIONS);
        }

        // 거래 없으면 실제 삭제 허용
        accountRepository.delete(accountEntity);
    }

    /**
     * 계좌 입금
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AccountBalanceResponseDTO deposit(String accountNumber, BigDecimal amount) {
        if (accountNumber == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransferSystemException(ErrorCode.INVALID_REQUEST);
        }

        AccountEntity accountEntity = accountRepository.findByAccountNumberLock(accountNumber)
            .orElseThrow(() -> new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));
        accountEntity.addBalance(amount);
        accountRepository.save(accountEntity);

        // 입금 거래 기록 저장
        TransactionEntity transactionEntity = TransactionEntity.builder()
            .fromAccount(null)
            .toAccount(accountEntity)
            .transactionType(TransactionType.DEPOSIT)
            .amount(amount)
            .fee(BigDecimal.ZERO)
            .createdTimeStamp(TimeUtils.nowKstLocalDateTime())
            .build();

        transactionRepository.save(transactionEntity);

        return AccountBalanceResponseDTO.builder()
            .accountNumber(accountEntity.getAccountNumber())
            .amount(MoneyUtils.normalize(amount))
            .balance(MoneyUtils.normalize(accountEntity.getBalance()))
            .build();
    }

    /**
     * 계좌 출금
     */
    @Override
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public AccountBalanceResponseDTO withdraw(String accountNumber, BigDecimal amount) {
        if (accountNumber == null || amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransferSystemException(ErrorCode.INVALID_REQUEST);
        }

        AccountEntity accountEntity = accountRepository.findByAccountNumberLock(accountNumber)
            .orElseThrow(() -> new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));

        LocalDateTime startTime = TimeUtils.startOfTodayKst();
        LocalDateTime endTime   = TimeUtils.endOfTodayKst();

        BigDecimal todayUsed = transactionRepository.getSumTodayUsedAmount(accountNumber, TransactionType.WITHDRAW, startTime, endTime);
        todayUsed = todayUsed != null ? todayUsed : BigDecimal.ZERO;

        transferPolicy.validateWithdrawAmount(amount, todayUsed);

        if (accountEntity.getBalance().compareTo(amount) < 0) {
            throw new TransferSystemException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        accountEntity.subtractBalance(amount);
        accountRepository.save(accountEntity);

        // 출금 거래 기록 저장
        TransactionEntity transactionEntity = TransactionEntity.builder()
            .fromAccount(accountEntity)
            .toAccount(null)
            .transactionType(TransactionType.WITHDRAW)
            .amount(amount)
            .fee(BigDecimal.ZERO)
            .createdTimeStamp(TimeUtils.nowKstLocalDateTime())
            .build();

        transactionRepository.save(transactionEntity);

        return AccountBalanceResponseDTO.builder()
            .accountNumber(accountEntity.getAccountNumber())
            .amount(MoneyUtils.normalize(amount))
            .balance(MoneyUtils.normalize(accountEntity.getBalance()))
            .build();
    }

    /**
     * Entity를 DTO로 변환
     */
    private AccountResponseDTO toDto(AccountEntity e) {
        return AccountResponseDTO.builder()
            .accountId(e.getAccountId())
            .accountNumber(e.getAccountNumber())
            .accountName(e.getAccountName())
            .bankName(e.getBankName())
            .accountType(e.getAccountType())
            .currencyType(e.getCurrencyType())
            .balance(MoneyUtils.normalize(e.getBalance()))
            .accountStatus(e.getAccountStatus())
            .createdTimeStamp(e.getCreatedTimeStamp())
            .updatedTimeStamp(e.getUpdatedTimeStamp())
            .build();
    }
}