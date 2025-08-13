package com.transfer.system.service;

import com.transfer.system.dto.TransactionRequestDTO;
import com.transfer.system.dto.TransactionResponseDTO;
import org.springframework.data.domain.Page;


public interface TransactionService {
    TransactionResponseDTO transfer(TransactionRequestDTO transactionRequestDTO);

    Page<TransactionResponseDTO> getTransactionHistory(String accountNumber, int page, int size);
}