package com.example.bankcards.controller;

import com.example.bankcards.entity.Transaction;
import com.example.bankcards.exception.CardOperationException;
import com.example.bankcards.exception.GlobalExceptionHandler;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.service.TransactionService;
import com.example.bankcards.service.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TransactionService transactionService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private TransactionController transactionController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(transactionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void transferBetweenMyCards_ShouldReturnTransaction() throws Exception {
        // Arrange
        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setAmount(new BigDecimal("100.00"));

        when(transactionService.transferBetweenMyCards(anyLong(), anyLong(), any(BigDecimal.class), anyString()))
                .thenReturn(transaction);

        // Act & Assert
        mockMvc.perform(post("/transactions/transfer/my-cards")
                        .param("fromCardId", "1")
                        .param("toCardId", "2")
                        .param("amount", "100.00")
                        .param("description", "Test transfer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(100.00));
    }

    @Test
    void transferBetweenMyCards_WithoutDescription_ShouldReturnTransaction() throws Exception {
        // Arrange
        Transaction transaction = new Transaction();
        transaction.setId(1L);
        transaction.setAmount(new BigDecimal("50.00"));

        when(transactionService.transferBetweenMyCards(anyLong(), anyLong(), any(BigDecimal.class), isNull()))
                .thenReturn(transaction);

        // Act & Assert
        mockMvc.perform(post("/transactions/transfer/my-cards")
                        .param("fromCardId", "1")
                        .param("toCardId", "2")
                        .param("amount", "50.00"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.amount").value(50.00));
    }

    @Test
    void transferBetweenMyCards_WithAccessDenied_ShouldReturnForbidden() throws Exception {
        // Arrange
        when(transactionService.transferBetweenMyCards(anyLong(), anyLong(), any(BigDecimal.class), anyString()))
                .thenThrow(new AccessDeniedException("Source card not found or access denied"));

        // Act & Assert
        mockMvc.perform(post("/transactions/transfer/my-cards")
                        .param("fromCardId", "1")
                        .param("toCardId", "2")
                        .param("amount", "100.00")
                        .param("description", "Test"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You don't have permission to access this resource"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Access Denied"));
    }

    @Test
    void transferBetweenMyCards_WithInactiveCard_ShouldReturnBadRequest() throws Exception {
        // Arrange
        when(transactionService.transferBetweenMyCards(anyLong(), anyLong(), any(BigDecimal.class), anyString()))
                .thenThrow(new CardOperationException("Source card is not active"));

        // Act & Assert
        mockMvc.perform(post("/transactions/transfer/my-cards")
                        .param("fromCardId", "1")
                        .param("toCardId", "2")
                        .param("amount", "100.00")
                        .param("description", "Test"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Source card is not active"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Card Operation Failed"));
    }

    @Test
    void transferBetweenMyCards_WithInsufficientFunds_ShouldReturnBadRequest() throws Exception {
        // Arrange
        when(transactionService.transferBetweenMyCards(anyLong(), anyLong(), any(BigDecimal.class), anyString()))
                .thenThrow(new InsufficientFundsException("Insufficient funds on source card. Available: 50.00"));

        // Act & Assert
        mockMvc.perform(post("/transactions/transfer/my-cards")
                        .param("fromCardId", "1")
                        .param("toCardId", "2")
                        .param("amount", "100.00")
                        .param("description", "Test"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Insufficient funds on source card. Available: 50.00"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Insufficient Funds"));
    }

    @Test
    void getMyTransactions_ShouldReturnTransactions() throws Exception {
        // Arrange
        Transaction transaction1 = new Transaction();
        transaction1.setId(1L);
        transaction1.setAmount(new BigDecimal("100.00"));

        Transaction transaction2 = new Transaction();
        transaction2.setId(2L);
        transaction2.setAmount(new BigDecimal("50.00"));

        when(transactionService.muTransactions()).thenReturn(List.of(transaction1, transaction2));

        // Act & Assert
        mockMvc.perform(get("/transactions/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].amount").value(100.00))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].amount").value(50.00));
    }

    @Test
    void getMyTransactions_EmptyList_ShouldReturnEmptyArray() throws Exception {
        // Arrange
        when(transactionService.muTransactions()).thenReturn(List.of());

        // Act & Assert
        mockMvc.perform(get("/transactions/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$").isEmpty());
    }

}