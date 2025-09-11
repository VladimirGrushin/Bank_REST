package com.example.bankcards.controller;

import com.example.bankcards.entity.BankCard;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.exception.CardOperationException;
import com.example.bankcards.exception.GlobalExceptionHandler;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.service.BankCardService;
import com.example.bankcards.service.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class BankCardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private BankCardService bankCardService;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @InjectMocks
    private BankCardController bankCardController;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private BankCard testCard;
    private BankCard testCard2;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(bankCardController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        testCard = new BankCard();
        testCard.setId(1L);
        testCard.setCardOwnerName("John Doe");
        testCard.setValidityPeriod(LocalDate.now());
        testCard.setStatus(CardStatus.ACTIVE);
        testCard.setBalance(new BigDecimal("1000.00"));
        testCard.setMaskedCardNumber("**** **** **** 1234");

        testCard2 = new BankCard();
        testCard2.setId(2L);
        testCard2.setCardOwnerName("Jane Smith");
        testCard2.setValidityPeriod(LocalDate.now());
        testCard2.setStatus(CardStatus.BLOCKED);
        testCard2.setBalance(new BigDecimal("500.00"));
        testCard2.setMaskedCardNumber("**** **** **** 5678");
    }

    @Test
    void getMyCards_ShouldReturnCards() throws Exception {
        // Arrange
        when(bankCardService.getMyCards()).thenReturn(Arrays.asList(testCard, testCard2));

        // Act & Assert
        mockMvc.perform(get("/cards/my"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].cardOwnerName").value("John Doe"))
                .andExpect(jsonPath("$[0].maskedCardNumber").value("**** **** **** 1234"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].cardOwnerName").value("Jane Smith"));
    }

    @Test
    void createCard_ShouldReturnCreatedCard() throws Exception {
        // Arrange
        when(bankCardService.createNewCard(anyString(), anyString(), anyLong())).thenReturn(testCard);

        // Act & Assert
        mockMvc.perform(post("/cards/admin/create")
                        .param("cardNumber", "1234567812345678")
                        .param("cardOwnerName", "John Doe")
                        .param("ownerId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.cardOwnerName").value("John Doe"));
    }

    @Test
    void getCardNumberForAdmin_ShouldReturnCardNumber() throws Exception {
        // Arrange
        when(bankCardService.getCardNumberForAdmin(anyLong())).thenReturn("1234567812345678");

        // Act & Assert
        mockMvc.perform(get("/cards/admin/1/number"))
                .andExpect(status().isOk())
                .andExpect(content().string("1234567812345678"));
    }

    @Test
    void activateCard_ShouldReturnActivatedCard() throws Exception {
        // Arrange
        when(bankCardService.activateCardByAdmin(anyLong())).thenReturn(testCard);

        // Act & Assert
        mockMvc.perform(patch("/cards/admin/1/activate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void deleteCard_ShouldReturnOk() throws Exception {
        // Arrange
        doNothing().when(bankCardService).deleteCardByAdmin(anyLong());

        // Act & Assert
        mockMvc.perform(delete("/cards/admin/1"))
                .andExpect(status().isOk());
    }

    @Test
    void approveBlockRequest_ShouldReturnCard() throws Exception {
        // Arrange
        testCard.setStatus(CardStatus.BLOCKED);
        when(bankCardService.approveBlockRequest(anyLong(), anyString())).thenReturn(testCard);

        // Act & Assert
        mockMvc.perform(patch("/cards/admin/1/approve-block")
                        .param("reason", "Suspicious activity"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    void rejectBlockRequest_ShouldReturnCard() throws Exception {
        // Arrange
        when(bankCardService.rejectBlockRequest(anyLong())).thenReturn(testCard);

        // Act & Assert
        mockMvc.perform(patch("/cards/admin/1/reject-block"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void blockCard_ShouldReturnBlockedCard() throws Exception {
        // Arrange
        testCard.setStatus(CardStatus.BLOCKED);
        when(bankCardService.blockCard(anyLong(), anyString())).thenReturn(testCard);

        // Act & Assert
        mockMvc.perform(patch("/cards/admin/1/block")
                        .param("reason", "Fraud detected"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("BLOCKED"));
    }

    @Test
    void requestBlockCard_ShouldReturnCard() throws Exception {
        // Arrange
        when(bankCardService.requestBlockCard(anyLong(), anyString())).thenReturn(testCard);

        // Act & Assert
        mockMvc.perform(patch("/cards/1/request-block")
                        .param("reason", "Lost card"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void cancelBlockRequest_ShouldReturnCard() throws Exception {
        // Arrange
        when(bankCardService.cancelRequestBlockCard(anyLong())).thenReturn(testCard);

        // Act & Assert
        mockMvc.perform(patch("/cards/1/cancel-block-request"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void getCardBalance_ShouldReturnBalance() throws Exception {
        // Arrange
        when(bankCardService.getCardBalance(anyLong())).thenReturn(new BigDecimal("1000.00"));

        // Act & Assert
        mockMvc.perform(get("/cards/1/balance"))
                .andExpect(status().isOk())
                .andExpect(content().string("1000.00"));
    }

    @Test
    void getCardsByStatus_ShouldReturnCards() throws Exception {
        // Arrange
        when(bankCardService.getCardsByStatus(any(CardStatus.class))).thenReturn(Arrays.asList(testCard));

        // Act & Assert
        mockMvc.perform(get("/cards/admin/status/ACTIVE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].status").value("ACTIVE"));
    }

    @Test
    void getAllCards_ShouldReturnAllCards() throws Exception {
        // Arrange
        when(bankCardService.getAllCards()).thenReturn(Arrays.asList(testCard, testCard2));

        // Act & Assert
        mockMvc.perform(get("/cards/admin/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[1].id").value(2));
    }

    @Test
    void getCardById_ShouldReturnCard() throws Exception {
        // Arrange
        when(bankCardService.getCardById(anyLong())).thenReturn(testCard);

        // Act & Assert
        mockMvc.perform(get("/cards/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.cardOwnerName").value("John Doe"));
    }

    @Test
    void getCardById_WithAccessDenied_ShouldReturnForbidden() throws Exception {
        // Arrange
        when(bankCardService.getCardById(anyLong()))
                .thenThrow(new AccessDeniedException("Access denied"));

        // Act & Assert
        mockMvc.perform(get("/cards/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You don't have permission to access this resource"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Access Denied"));
    }

    @Test
    void getCardById_WithNotFound_ShouldReturnNotFound() throws Exception {
        // Arrange
        when(bankCardService.getCardById(anyLong()))
                .thenThrow(new ResourceNotFoundException("Card", "id", 1L));

        // Act & Assert
        mockMvc.perform(get("/cards/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Card not found with id: '1'"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Resource Not Found"));
    }

    @Test
    void activateCard_WithExpiredCard_ShouldReturnBadRequest() throws Exception {
        // Arrange
        when(bankCardService.activateCardByAdmin(anyLong()))
                .thenThrow(new CardOperationException("Cannot activate expired card"));

        // Act & Assert
        mockMvc.perform(patch("/cards/admin/1/activate"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot activate expired card"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Card Operation Failed"));
    }

    @Test
    void deleteCard_WithNonZeroBalance_ShouldReturnBadRequest() throws Exception {
        // Arrange
        doThrow(new CardOperationException("Cannot delete card with non-zero balance"))
                .when(bankCardService).deleteCardByAdmin(anyLong());

        // Act & Assert
        mockMvc.perform(delete("/cards/admin/1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Cannot delete card with non-zero balance"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Card Operation Failed"));
    }

    @Test
    void approveBlockRequest_WithNoPendingRequest_ShouldReturnBadRequest() throws Exception {
        // Arrange
        when(bankCardService.approveBlockRequest(anyLong(), anyString()))
                .thenThrow(new CardOperationException("No block request pending for this card"));

        // Act & Assert
        mockMvc.perform(patch("/cards/admin/1/approve-block")
                        .param("reason", "Test reason"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("No block request pending for this card"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Card Operation Failed"));
    }

    @Test
    void requestBlockCard_WithAlreadyBlocked_ShouldReturnBadRequest() throws Exception {
        // Arrange
        when(bankCardService.requestBlockCard(anyLong(), anyString()))
                .thenThrow(new CardOperationException("Card is already blocked"));

        // Act & Assert
        mockMvc.perform(patch("/cards/1/request-block")
                        .param("reason", "Test reason"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Card is already blocked"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Card Operation Failed"));
    }
}