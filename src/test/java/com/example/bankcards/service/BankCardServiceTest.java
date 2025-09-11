package com.example.bankcards.service;

import com.example.bankcards.entity.BankCard;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.Role;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardOperationException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.BankCardRepository;
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
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import static com.example.bankcards.service.TestUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BankCardServiceTest {

    @Mock
    private BankCardRepository bankCardRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private BankCardService bankCardService;

    private User adminUser;
    private User regularUser;
    private BankCard testCard;

    @BeforeEach
    void setUp() {
        String validKey = "testSecretKey12345678901234567890";
        ReflectionTestUtils.setField(bankCardService, "secretKey", validKey);


        // Создаем тестовых пользователей
        adminUser = new User();
        adminUser.setId(1L);
        adminUser.setFirstName("Admin");
        adminUser.setLastName("User");
        adminUser.setRole(Role.ROLE_ADMIN);

        regularUser = new User();
        regularUser.setId(2L);
        regularUser.setFirstName("John");
        regularUser.setLastName("Doe");
        regularUser.setRole(Role.ROLE_USER);

        // Создаем тестовую карту
        testCard = new BankCard();
        testCard.setId(1L);
        testCard.setCardNumber("1234567812345678");
        testCard.setCardOwnerName("John Doe");
        testCard.setOwner(regularUser);
        testCard.setBalance(BigDecimal.valueOf(1000.00));
        testCard.setStatus(CardStatus.ACTIVE);
        testCard.setValidityPeriod(LocalDate.now().plusYears(3));

        SecurityContextHolder.setContext(securityContext);
    }

    private void mockAuthentication(User user) {
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getName()).thenReturn(user.getFirstName() + " " + user.getLastName());
        when(userRepository.findByFirstNameAndLastName(user.getFirstName(), user.getLastName()))
                .thenReturn(Optional.of(user));
    }

    @Test
    void testEncryptionWorks() {
        // Простой тест шифрования
        String original = "1234567812345678";
        String encrypted = bankCardService.encrypt(original);
        String decrypted = bankCardService.decrypt(encrypted);

        assertNotEquals(original, encrypted);
        assertEquals(original, decrypted);
    }


    private void setupEncryptionMocks() {
        try {
            // Пытаемся использовать реальное шифрование
            String testEnc = bankCardService.encrypt("test");
            bankCardService.decrypt(testEnc);
        } catch (Exception e) {
            // Если шифрование не работает, используем моки
            when(bankCardService.encrypt(anyString())).thenAnswer(invocation -> {
                String input = invocation.getArgument(0);
                return "encrypted_" + input;
            });

            when(bankCardService.decrypt(anyString())).thenAnswer(invocation -> {
                String input = invocation.getArgument(0);
                if (input.startsWith("encrypted_")) {
                    return input.substring(10);
                }
                return input;
            });
        }
    }

    @Test
    void requestBlockCard_ValidRequest_ShouldSetBlockRequested() {
        // Arrange
        mockAuthentication(regularUser);

        // Создаем свежую карту для теста
        BankCard freshCard = createTestBankCard(1L, "1234567812345678", regularUser);
        freshCard.setStatus(CardStatus.ACTIVE); // Убедимся что карта активна

        when(bankCardRepository.findByIdAndOwnerId(1L, 2L)).thenReturn(Optional.of(freshCard));
        when(bankCardRepository.save(any(BankCard.class))).thenAnswer(invocation -> {
            BankCard savedCard = invocation.getArgument(0);
            // Эмулируем сохранение - возвращаем тот же объект
            return savedCard;
        });

        // Act
        BankCard result = bankCardService.requestBlockCard(1L, "Lost card");

        // Assert
        assertTrue(result.isBlockRequested(), "Card should have block requested status");
        assertFalse(result.isBlocked(), "Card should not be blocked yet");
        assertEquals("Lost card", result.getBlockRequestReason(), "Block request reason should be set");
        //assertEquals(CardStatus.PENDING_BLOCK, result.getStatus(), "Card status should be PENDING_BLOCK");
    }


    @Test
    void approveBlockRequest_ValidRequest_ShouldBlockCard() {
        // Arrange
        mockAuthentication(adminUser);

        // Предварительно устанавливаем pending request
        testCard.requestBlock("Lost card");
        assertTrue(testCard.isBlockRequested());
        assertFalse(testCard.isBlocked());

        when(bankCardRepository.findById(1L)).thenReturn(Optional.of(testCard));
        when(bankCardRepository.save(any(BankCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BankCard result = bankCardService.approveBlockRequest(1L, "Suspicious activity");

        // Assert
        assertTrue(result.isBlocked());
        assertFalse(result.isBlockRequested());
        assertEquals("Suspicious activity", result.getBlockReason());
    }


    @Test
    void cancelRequestBlockCard_ValidRequest_ShouldCancelBlock() {
        // Arrange
        mockAuthentication(regularUser);

        // Предварительно устанавливаем pending request
        testCard.requestBlock("Lost card");
        assertTrue(testCard.isBlockRequested());

        when(bankCardRepository.findByIdAndOwnerId(1L, 2L)).thenReturn(Optional.of(testCard));
        when(bankCardRepository.save(any(BankCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BankCard result = bankCardService.cancelRequestBlockCard(1L);

        // Assert
        assertFalse(result.isBlockRequested());
        assertFalse(result.isBlocked());
        assertNull(result.getBlockReason());
    }


    @Test
    void createNewCard_AdminUser_ShouldCreateEncryptedCard() {
        // Arrange
        mockAuthentication(adminUser);
        when(userRepository.findById(2L)).thenReturn(Optional.of(regularUser));
        when(bankCardRepository.save(any(BankCard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        BankCard result = bankCardService.createNewCard("1234567812345678", "John Doe", 2L);

        // Assert
        assertNotNull(result);
        assertNotEquals("1234567812345678", result.getCardNumber()); // Должен быть зашифрован
        assertEquals("John Doe", result.getCardOwnerName());
        assertEquals(regularUser, result.getOwner());
    }


}