package com.transfer.system.controller;

import com.transfer.system.dto.AccountBalanceRequestDTO;
import com.transfer.system.dto.AccountCreateRequestDTO;
import com.transfer.system.dto.AccountResponseDTO;
import com.transfer.system.dto.CommonResponseDTO;
import com.transfer.system.enums.ResponseMessage;
import com.transfer.system.service.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {
    private final AccountService accountService;

    @PostMapping("/create")
    public ResponseEntity<CommonResponseDTO<AccountResponseDTO>> createAccount(@RequestBody AccountCreateRequestDTO AccountCreateRequestDTO) {
        AccountResponseDTO response = accountService.createAccount(AccountCreateRequestDTO);

        return ResponseEntity.ok(CommonResponseDTO.successHasData(response, ResponseMessage.ACCOUNT_CREATED.getMessage()));
    }

    @GetMapping("/{accountId}")
    public ResponseEntity<CommonResponseDTO<AccountResponseDTO>> getAccount(@PathVariable UUID accountId) {
        AccountResponseDTO response = accountService.getAccount(accountId);

        return ResponseEntity.ok(CommonResponseDTO.successHasData(response, ResponseMessage.ACCOUNT_RETRIEVED.getMessage()));
    }

    @DeleteMapping("/{accountId}")
    public ResponseEntity<CommonResponseDTO<Void>> deleteAccount(@PathVariable UUID accountId) {
        accountService.deleteAccount(accountId);

        return ResponseEntity.ok(CommonResponseDTO.successNoData(ResponseMessage.ACCOUNT_DELETED.getMessage()));
    }

    @PostMapping("/deposit")
    public ResponseEntity<CommonResponseDTO<Void>> deposit(@RequestBody AccountBalanceRequestDTO accountBalanceRequestDTO) {
        accountService.deposit(accountBalanceRequestDTO.getAccountNumber(), accountBalanceRequestDTO.getAmount());

        return ResponseEntity.ok(CommonResponseDTO.successNoData(ResponseMessage.TRANSFER_SUCCESSFUL.getMessage()));
    }

    @PostMapping("/withdraw")
    public ResponseEntity<CommonResponseDTO<Void>> withdraw(@RequestBody AccountBalanceRequestDTO accountBalanceRequestDTO) {
        accountService.withdraw((accountBalanceRequestDTO.getAccountNumber()), accountBalanceRequestDTO.getAmount());

        return ResponseEntity.ok(CommonResponseDTO.successNoData(ResponseMessage.WITHDRAW_SUCCESSFUL.getMessage()));
    }
}