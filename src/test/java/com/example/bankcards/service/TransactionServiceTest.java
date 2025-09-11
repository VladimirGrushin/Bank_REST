package com.example.bankcards.service;

import com.example.bankcards.entity.*;
import com.example.bankcards.exception.CardOperationException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.BankCardRepository;
import com.example.bankcards.repository.TransactionRepository;
import com.example.bankcards.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private BankCardRepository bankCardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private TransactionService transactionService;

    private User testUser;
    private BankCard fromCard;
    private BankCard toCard;

    @BeforeEach
    void setUp() {
        // Создаем тестового пользователя
        testUser = new User();
        testUser.setId(2L);
        testUser.setFirstName("John");
        testUser.setLastName("Doe");
        testUser.setRole(Role.ROLE_USER);

        fromCard = new BankCard();
        fromCard.setId(1L);
        fromCard.setCardNumber("1234567812345678");
        fromCard.setCardOwnerName(testUser.getFirstName() + " " + testUser.getLastName());
        fromCard.setOwner(testUser);
        fromCard.setBalance(BigDecimal.valueOf(1000.00));
        fromCard.setStatus(CardStatus.ACTIVE);
        fromCard.setValidityPeriod(LocalDate.now().plusYears(3));

        toCard = new BankCard();
        toCard.setId(2L);
        toCard.setCardNumber("8765432187654321");
        toCard.setCardOwnerName(testUser.getFirstName() + " " + testUser.getLastName());
        toCard.setOwner(testUser);
        toCard.setBalance(BigDecimal.valueOf(500.00));
        toCard.setStatus(CardStatus.ACTIVE);
        toCard.setValidityPeriod(LocalDate.now().plusYears(3));

        // Активируем карты
        fromCard.activateCard();
        toCard.activateCard();

        SecurityContextHolder.setContext(securityContext);
    }

    private void mockAuthentication(User user) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(user.getFirstName() + " " + user.getLastName());
        when(userRepository.findByFirstNameAndLastName(user.getFirstName(), user.getLastName()))
                .thenReturn(Optional.of(user));
    }

    @Test
    void transferBetweenMyCards_ValidTransfer_ShouldCompleteSuccessfully() {
        // Arrange
        mockAuthentication(testUser);

        // Используем правильные ID: карта ID и пользователь ID (2L)
        when(bankCardRepository.findByIdAndOwnerId(eq(1L), eq(2L))).thenReturn(Optional.of(fromCard));
        when(bankCardRepository.findByIdAndOwnerId(eq(2L), eq(2L))).thenReturn(Optional.of(toCard));

        when(bankCardRepository.save(any(BankCard.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(1L);
            return transaction;
        });

        BigDecimal transferAmount = new BigDecimal("100.00");

        // Act
        Transaction result = transactionService.transferBetweenMyCards(1L, 2L, transferAmount, "Test transfer");

        // Assert
        assertNotNull(result);
        assertEquals(transferAmount, result.getAmount());
        assertEquals(fromCard, result.getFromCard());
        assertEquals(toCard, result.getToCard());
        assertEquals("Test transfer", result.getDescription());

        // Проверяем что балансы изменились
        assertEquals(new BigDecimal("900.00"), fromCard.getBalance());
        assertEquals(new BigDecimal("600.00"), toCard.getBalance());

        verify(bankCardRepository, times(2)).save(any(BankCard.class));
        verify(transactionRepository).save(any(Transaction.class));
    }

    @Test
    void transferBetweenMyCards_InsufficientFunds_ShouldThrowException() {
        // Arrange
        mockAuthentication(testUser);

        when(bankCardRepository.findByIdAndOwnerId(eq(1L), eq(2L))).thenReturn(Optional.of(fromCard));
        when(bankCardRepository.findByIdAndOwnerId(eq(2L), eq(2L))).thenReturn(Optional.of(toCard));

        BigDecimal transferAmount = new BigDecimal("1500.00"); // Больше чем на карте

        // Act & Assert
        assertThrows(InsufficientFundsException.class, () ->
                transactionService.transferBetweenMyCards(1L, 2L, transferAmount, "Test transfer"));
    }

    @Test
    void transferBetweenMyCards_SourceCardNotActive_ShouldThrowException() {
        // Arrange
        mockAuthentication(testUser);

        fromCard.blockCard("Test block"); // Блокируем исходную карту

        when(bankCardRepository.findByIdAndOwnerId(eq(1L), eq(2L))).thenReturn(Optional.of(fromCard));
        when(bankCardRepository.findByIdAndOwnerId(eq(2L), eq(2L))).thenReturn(Optional.of(toCard));

        // Act & Assert
        assertThrows(CardOperationException.class, () ->
                transactionService.transferBetweenMyCards(1L, 2L, new BigDecimal("100.00"), "Test transfer"));
    }

    @Test
    void transferBetweenMyCards_DestinationCardNotActive_ShouldThrowException() {
        // Arrange
        mockAuthentication(testUser);

        toCard.blockCard("Test block"); // Блокируем целевую карту

        when(bankCardRepository.findByIdAndOwnerId(eq(1L), eq(2L))).thenReturn(Optional.of(fromCard));
        when(bankCardRepository.findByIdAndOwnerId(eq(2L), eq(2L))).thenReturn(Optional.of(toCard));

        // Act & Assert
        assertThrows(CardOperationException.class, () ->
                transactionService.transferBetweenMyCards(1L, 2L, new BigDecimal("100.00"), "Test transfer"));
    }

    @Test
    void transferBetweenMyCards_SourceCardNotFound_ShouldThrowAccessDenied() {
        // Arrange
        mockAuthentication(testUser);

        when(bankCardRepository.findByIdAndOwnerId(eq(1L), eq(2L))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AccessDeniedException.class, () ->
                transactionService.transferBetweenMyCards(1L, 2L, new BigDecimal("100.00"), "Test transfer"));
    }

    @Test
    void transferBetweenMyCards_DestinationCardNotFound_ShouldThrowAccessDenied() {
        // Arrange
        mockAuthentication(testUser);

        when(bankCardRepository.findByIdAndOwnerId(eq(1L), eq(2L))).thenReturn(Optional.of(fromCard));
        when(bankCardRepository.findByIdAndOwnerId(eq(2L), eq(2L))).thenReturn(Optional.empty());

        // Act & Assert
        assertThrows(AccessDeniedException.class, () ->
                transactionService.transferBetweenMyCards(1L, 2L, new BigDecimal("100.00"), "Test transfer"));
    }

    @Test
    void transferBetweenMyCards_NullDescription_ShouldWork() {
        // Arrange
        mockAuthentication(testUser);

        when(bankCardRepository.findByIdAndOwnerId(eq(1L), eq(2L))).thenReturn(Optional.of(fromCard));
        when(bankCardRepository.findByIdAndOwnerId(eq(2L), eq(2L))).thenReturn(Optional.of(toCard));

        when(bankCardRepository.save(any(BankCard.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(transactionRepository.save(any(Transaction.class))).thenAnswer(invocation -> {
            Transaction transaction = invocation.getArgument(0);
            transaction.setId(1L);
            return transaction;
        });

        // Act
        Transaction result = transactionService.transferBetweenMyCards(1L, 2L, new BigDecimal("100.00"), null);

        // Assert
        assertNotNull(result);
        assertNull(result.getDescription());
    }

    @Test
    void myTransactions_ShouldReturnUserTransactions() {
        // Arrange
        mockAuthentication(testUser);

        Transaction transaction1 = new Transaction();
        transaction1.setId(1L);
        transaction1.setAmount(new BigDecimal("100.00"));
        transaction1.setFromCard(fromCard);
        transaction1.setToCard(toCard);

        Transaction transaction2 = new Transaction();
        transaction2.setId(2L);
        transaction2.setAmount(new BigDecimal("200.00"));
        transaction2.setFromCard(toCard);
        transaction2.setToCard(fromCard);

        when(transactionRepository.findByFromCardOwnerIdOrToCardOwnerId(eq(2L), eq(2L)))
                .thenReturn(List.of(transaction1, transaction2));

        // Act
        List<Transaction> result = transactionService.muTransactions();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());
        verify(transactionRepository).findByFromCardOwnerIdOrToCardOwnerId(eq(2L), eq(2L));
    }

    @Test
    void myTransactions_NoTransactions_ShouldReturnEmptyList() {
        // Arrange
        mockAuthentication(testUser);

        when(transactionRepository.findByFromCardOwnerIdOrToCardOwnerId(eq(2L), eq(2L)))
                .thenReturn(List.of());

        // Act
        List<Transaction> result = transactionService.muTransactions();

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void transferBetweenMyCards_ZeroAmount_ShouldThrowException() {
        // Arrange
        mockAuthentication(testUser);

        when(bankCardRepository.findByIdAndOwnerId(eq(1L), eq(2L))).thenReturn(Optional.of(fromCard));
        when(bankCardRepository.findByIdAndOwnerId(eq(2L), eq(2L))).thenReturn(Optional.of(toCard));

        // Act & Assert
        assertThrows(CardOperationException.class, () ->
                transactionService.transferBetweenMyCards(1L, 2L, BigDecimal.ZERO, "Test transfer"));
    }

    @Test
    void transferBetweenMyCards_NegativeAmount_ShouldThrowException() {
        // Arrange
        mockAuthentication(testUser);

        when(bankCardRepository.findByIdAndOwnerId(eq(1L), eq(2L))).thenReturn(Optional.of(fromCard));
        when(bankCardRepository.findByIdAndOwnerId(eq(2L), eq(2L))).thenReturn(Optional.of(toCard));

        // Act & Assert
        assertThrows(CardOperationException.class, () ->
                transactionService.transferBetweenMyCards(1L, 2L, new BigDecimal("-100.00"), "Test transfer"));
    }
}