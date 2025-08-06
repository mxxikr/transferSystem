package com.transfer.system.service;

import com.transfer.system.domain.AccountEntity;
import com.transfer.system.domain.TransactionEntity;
import com.transfer.system.dto.TransactionRequestDTO;
import com.transfer.system.dto.TransactionResponseDTO;
import com.transfer.system.enums.AccountStatus;
import com.transfer.system.enums.TransactionType;
import com.transfer.system.exception.ErrorCode;
import com.transfer.system.exception.TransferSystemException;
import com.transfer.system.policy.TransferPolicy;
import com.transfer.system.repository.AccountRepository;
import com.transfer.system.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class TransactionServiceImpl implements TransactionService {
    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransferPolicy transferPolicy;

    public TransactionServiceImpl(AccountRepository accountRepository, TransactionRepository transactionRepository, TransferPolicy transferPolicy) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.transferPolicy = transferPolicy;
    }

    /**
     * 이체 기능
     *
     * @return
     */
    @Override
    @Transactional
    public TransactionResponseDTO transfer(TransactionRequestDTO transactionRequestDTO) {
        if (transactionRequestDTO == null) {
            throw new TransferSystemException(ErrorCode.INVALID_REQUEST);
        }

        if (transactionRequestDTO.getFromAccountNumber() == null || transactionRequestDTO.getToAccountNumber() == null) {
            throw new TransferSystemException(ErrorCode.INVALID_ACCOUNT_NUMBER);
        }

        if (transactionRequestDTO.getFromAccountNumber().equals(transactionRequestDTO.getToAccountNumber())) { // 같은 계좌로는 이체할 수 없음
            throw new TransferSystemException(ErrorCode.TRANSFER_SAME_ACCOUNT);
        }

        // 계좌 존재하는지 확인
        AccountEntity fromAccount = accountRepository.findByAccountNumber(
                transactionRequestDTO.getFromAccountNumber()).orElseThrow(() -> new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));

        AccountEntity toAccount = accountRepository.findByAccountNumber(
                transactionRequestDTO.getToAccountNumber()).orElseThrow(() -> new TransferSystemException(ErrorCode.ACCOUNT_NOT_FOUND));

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

        // 이체 금액 유효성 검사
        if (transactionRequestDTO.getAmount() == null || transactionRequestDTO.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new TransferSystemException(ErrorCode.INVALID_AMOUNT);
        }

        BigDecimal amount = transactionRequestDTO.getAmount();
        BigDecimal fee = transferPolicy.calculateFee(amount); // 이체 수수료 계산

        // 이체 수수료 유효성 검사
        if (fee == null || fee.compareTo(BigDecimal.ZERO) < 0) {
            throw new TransferSystemException(ErrorCode.INVALID_FEE);
        }

        BigDecimal total = amount.add(fee); // 총 이쳬 금액

        // 이체 한도 확인
        if (total.compareTo(transferPolicy.getTransferDailyLimit()) > 0) { // 하루 이체 한도를 초과하는지 확인
            throw new TransferSystemException(ErrorCode.TRANSFER_LIMIT_EXCEEDED);
        }

        // 잔액 확인
        if (fromAccount.getBalance().compareTo(total) < 0) { // 출금 계좌의 잔액이 이체 금액보다 많아야 함
            throw new TransferSystemException(ErrorCode.INSUFFICIENT_BALANCE);
        }

        BigDecimal updatedSenderBalance = fromAccount.getBalance().subtract(total);
        BigDecimal updatedReceiverBalance = toAccount.getBalance().add(amount);

        // 계좌 잔액 업데이트
        fromAccount.updateBalance(updatedSenderBalance);
        toAccount.updateBalance(updatedReceiverBalance);

        // 기록 저장
        TransactionEntity transactionEntity = TransactionEntity.builder()
                .fromAccount(fromAccount)
                .toAccount(toAccount)
                .transactionType(TransactionType.TRANSFER)
                .amount(amount)
                .fee(fee)
                .createdTimeStamp(LocalDateTime.now())
                .build();

        TransactionEntity savedTransactionEntity = transactionRepository.save(transactionEntity);
        return TransactionResponseDTO.from(savedTransactionEntity);
    }
}