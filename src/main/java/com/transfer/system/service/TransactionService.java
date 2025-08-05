package com.transfer.system.service;

import com.transfer.system.dto.TransactionRequestDTO;
import com.transfer.system.dto.TransactionResponseDTO;

public interface TransactionService {
    TransactionResponseDTO transfer(TransactionRequestDTO transactionRequestDTO);
}