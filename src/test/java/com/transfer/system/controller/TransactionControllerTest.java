package com.transfer.system.controller;

import com.transfer.system.dto.TransactionRequestDTO;
import com.transfer.system.dto.TransactionResponseDTO;
import com.transfer.system.enums.ResultCode;
import com.transfer.system.enums.ResponseMessage;
import com.transfer.system.enums.TransactionType;
import com.transfer.system.exception.ErrorCode;
import com.transfer.system.exception.GlobalExceptionHandler;
import com.transfer.system.exception.TransferSystemException;
import com.transfer.system.service.TransactionService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TransactionService transactionService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private TransactionRequestDTO transactionRequestDTO;
    private TransactionResponseDTO transactionResponseDTO;

    private final UUID testTransactionId = UUID.randomUUID();

    private static class Endpoint {
        static final String TRANSFER = "/api/transaction/transfer";
        static final String HISTORY = "/api/transaction/history";
    }

    @BeforeEach
    void setUp() {
        TransactionController transactionController = new TransactionController(transactionService);
        mockMvc = MockMvcBuilders.standaloneSetup(transactionController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

        transactionRequestDTO = TransactionRequestDTO.builder()
            .fromAccountNumber("account123")
            .toAccountNumber("account456")
            .amount(new BigDecimal("100000"))
            .build();

        transactionResponseDTO = TransactionResponseDTO.builder()
            .transactionId(testTransactionId)
            .fromAccountNumber("account123")
            .toAccountNumber("account456")
            .transactionType(TransactionType.TRANSFER)
            .amount(new BigDecimal("100000"))
            .fee(new BigDecimal("1000"))
            .createdTimeStamp(LocalDateTime.now())
            .build();
    }

    // ========================= 공통 메서드 =========================
    /**
     * 이체 요청 수행
     */
    private ResultActions performTransferRequest(TransactionRequestDTO dto) throws Exception {
        return mockMvc.perform(post(Endpoint.TRANSFER)
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(dto)));
    }

    /**
     * 거래 내역 조회 요청 수행
     */
    private ResultActions performHistoryRequest(String accountNumber, int page, int size) throws Exception {
        return mockMvc.perform(get(Endpoint.HISTORY)
            .param("accountNumber", accountNumber)
            .param("page", String.valueOf(page))
            .param("size", String.valueOf(size)));
    }

    /**
     * 이체 요청 시 에러 처리
     */
    private void expectTransferError(TransactionRequestDTO dto, ErrorCode errorCode, HttpStatus status) throws Exception {
        when(transactionService.transfer(any(TransactionRequestDTO.class)))
            .thenThrow(new TransferSystemException(errorCode));

        performTransferRequest(dto)
            .andExpect(status().is(status.value()))
            .andExpect(jsonPath("$.message").value(errorCode.getMessage()));
    }

    /**
     * 거래 내역 조회 시 에러 처리
     */
    private void expectHistoryError(String accountNumber, ErrorCode errorCode, HttpStatus status) throws Exception {
        when(transactionService.getTransactionHistory(anyString(), anyInt(), anyInt()))
        .thenThrow(new TransferSystemException(errorCode));

        performHistoryRequest(accountNumber, 0, 10)
        .andExpect(status().is(status.value()))
        .andExpect(jsonPath("$.message").value(errorCode.getMessage()));
    }

    // ========================= 이체 테스트 =========================
    @Nested
    class TransferTest {

        /**
         * 이체 성공 테스트
         */
        @Test
        void transfer_success() throws Exception {
            when(transactionService.transfer(any(TransactionRequestDTO.class)))
                .thenReturn(transactionResponseDTO);

            performTransferRequest(transactionRequestDTO)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.result_code").value(ResultCode.SUCCESS_HAS_DATA.getCode()))
                .andExpect(jsonPath("$.message").value(ResultCode.SUCCESS_HAS_DATA.getMessage()))
                .andExpect(jsonPath("$.data.fromAccountNumber").value("account123"))
                .andExpect(jsonPath("$.data.amount").value(100000));

            verify(transactionService).transfer(any(TransactionRequestDTO.class));
        }

        /**
         * JSON 형식이 잘못된 경우
         */
        @Test
        void transfer_invalidJson() throws Exception {
            mockMvc.perform(post(Endpoint.TRANSFER)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json"))
                .andExpect(status().isBadRequest());

            verify(transactionService, never()).transfer(any());
        }

        /**
         * 같은 계좌로 이체할 경우
         */
        @Test
        void transfer_sameAccount() throws Exception {
            TransactionRequestDTO requestDTO = TransactionRequestDTO.builder()
                .fromAccountNumber("account123")
                .toAccountNumber("account123")
                .amount(new BigDecimal("100000"))
                .build();

            expectTransferError(requestDTO, ErrorCode.TRANSFER_SAME_ACCOUNT, HttpStatus.BAD_REQUEST);
        }

        /**
         * 계좌번호가 존재하지 않는 경우
         */
        @Test
        void transfer_accountNotFound() throws Exception {
            expectTransferError(transactionRequestDTO, ErrorCode.ACCOUNT_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        /**
         * 잔액이 부족할 경우
         */
        @Test
        void transfer_insufficientBalance() throws Exception {
            expectTransferError(transactionRequestDTO, ErrorCode.INSUFFICIENT_BALANCE, HttpStatus.BAD_REQUEST);
        }

        /**
         * 일일 이체 한도 초과한 경우
         */
        @Test
        void transfer_limitExceeded() throws Exception {
            TransactionRequestDTO requestDTO = TransactionRequestDTO.builder()
                .fromAccountNumber("account123")
                .toAccountNumber("account456")
                .amount(new BigDecimal("5000000"))
                .build();

            expectTransferError(requestDTO, ErrorCode.TRANSFER_LIMIT_EXCEEDED, HttpStatus.BAD_REQUEST);
        }

        /**
         * 이체 요청 DTO의 통화 타입이 일치하지 않는 경우
         */
        @Test
        void transfer_currencyTypeMismatch() throws Exception {
            expectTransferError(transactionRequestDTO, ErrorCode.CURRENCY_TYPE_MISMATCH, HttpStatus.BAD_REQUEST);
        }

        /**
         * 이체 요청 DTO의 계좌가 비활성화된 경우
         */
        @Test
        void transfer_inactiveAccount() throws Exception {
            expectTransferError(transactionRequestDTO, ErrorCode.SENDER_ACCOUNT_INACTIVE, HttpStatus.BAD_REQUEST);
        }
    }

    // ========================= 거래 내역 조회 테스트 =========================
    @Nested
    class GetTransactionHistoryTest {

        /**
         * 거래 내역 조회 성공
         */
        @Test
        void getTransactionHistory_success() throws Exception {
            String accountNumber = "account123";
            List<TransactionResponseDTO> transactions = List.of(transactionResponseDTO);
            Page<TransactionResponseDTO> transactionPage = new PageImpl<>(
                transactions, PageRequest.of(0, 10), transactions.size());

            when(transactionService.getTransactionHistory(accountNumber, 0, 10))
                .thenReturn(transactionPage);

            performHistoryRequest(accountNumber, 0, 10)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result_code").value(ResultCode.SUCCESS_HAS_DATA.getCode()))
                .andExpect(jsonPath("$.message").value(ResultCode.SUCCESS_HAS_DATA.getMessage()))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].transactionId").value(testTransactionId.toString()));

            verify(transactionService).getTransactionHistory(accountNumber, 0, 10);
        }

        /**
         * 거래 내역이 없는 경우
         */
        @Test
        void getTransactionHistory_emptyResult() throws Exception {
            String accountNumber = "account123";
            Page<TransactionResponseDTO> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

            when(transactionService.getTransactionHistory(accountNumber, 0, 10))
                .thenReturn(emptyPage);

            performHistoryRequest(accountNumber, 0, 10)
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.result_code").value(ResultCode.SUCCESS_HAS_DATA.getCode()))
                .andExpect(jsonPath("$.message").value(ResultCode.SUCCESS_HAS_DATA.getMessage()))
                .andExpect(jsonPath("$.data.content.length()").value(0))
                .andExpect(jsonPath("$.data.totalElements").value(0));
        }

        /**
         * 계좌 번호가 잘못된 경우
         */
        @Test
        void getTransactionHistory_accountNotFound() throws Exception {
            expectHistoryError("nonexistent", ErrorCode.ACCOUNT_NOT_FOUND, HttpStatus.NOT_FOUND);
        }

        /**
         * 계좌 번호 누락
         */
        @Test
        void getTransactionHistory_missingAccountNumber() throws Exception {
            mockMvc.perform(get(Endpoint.HISTORY)
                .param("page", "0")
                .param("size", "10"))
                .andExpect(status().isBadRequest());
        }

        /**
         * 페이지 파라미터가 누락된 경우
         */
        @Test
        void getTransactionHistory_missingPageParams() throws Exception {
            mockMvc.perform(get(Endpoint.HISTORY)
                .param("accountNumber", "account123"))
                .andExpect(status().isBadRequest());
        }

        /**
         * 페이지 파라미터가 잘못된 경우
         */
        @Test
        void getTransactionHistory_invalidPageParams() throws Exception {
            mockMvc.perform(get(Endpoint.HISTORY)
                .param("accountNumber", "account123")
                .param("page", "invalid")
                .param("size", "10"))
                .andExpect(status().isBadRequest());
        }

        /**
         * 파라미터 누락
         */
        @Test
        void getTransactionHistory_missingRequiredParams() throws Exception {
            mockMvc.perform(get(Endpoint.HISTORY))
                .andExpect(status().isBadRequest());
        }
    }
}