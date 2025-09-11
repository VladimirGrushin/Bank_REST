package com.example.bankcards.service;

import com.example.bankcards.entity.*;
import com.example.bankcards.dto.request.AuthRequest;
import com.example.bankcards.dto.response.AuthResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class TestUtils {


    // В тестах я использую методы из сервисов, в процессе написания этого класса я решил от него отказаться по причине приближения дедлайна

    public static User createTestUser(String firstName, String lastName, Role role, String password) {
        User user = new User();
        user.setFirstName(firstName);
        user.setLastName(lastName);
        user.setRole(role);
        user.setPassword(password);
        user.setPassword("encoded-password-" + UUID.randomUUID());
        return user;
    }



    public static BankCard createTestBankCard(Long id, String cardNumber, User owner) {
        BankCard card = new BankCard();
        card.setId(id);
        card.setCardNumber(cardNumber);
        card.setCardOwnerName(owner.getFirstName() + " " + owner.getLastName());
        card.setOwner(owner);
        card.setBalance(BigDecimal.ZERO);
        card.setStatus(CardStatus.ACTIVE);
        card.setValidityPeriod(LocalDate.now().plusYears(3));
        card.setCreatedAt(LocalDateTime.now());
        return card;
    }

    public static BankCard createTestBankCard(Long id, String cardNumber, User owner, BigDecimal balance) {
        BankCard card = createTestBankCard(id, cardNumber, owner);
        card.setBalance(balance);
        return card;
    }

    public static BankCard createTestBankCard(Long id, String cardNumber, User owner, CardStatus status) {
        BankCard card = createTestBankCard(id, cardNumber, owner);
        card.setStatus(status);
        return card;
    }

    public static BankCard createTestBankCard(Long id, String cardNumber, User owner, BigDecimal balance, CardStatus status) {
        BankCard card = createTestBankCard(id, cardNumber, owner);
        card.setBalance(balance);
        card.setStatus(status);
        return card;
    }

    public static BankCard createExpiredBankCard(Long id, String cardNumber, User owner) {
        BankCard card = createTestBankCard(id, cardNumber, owner);
        card.setValidityPeriod(LocalDate.now().minusDays(1));
        return card;
    }

    public static Transaction createTestTransaction(Long id, BigDecimal amount, BankCard fromCard, BankCard toCard) {
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setAmount(amount);
        transaction.setFromCard(fromCard);
        transaction.setToCard(toCard);
        transaction.setTimestamp(LocalDateTime.now());
        transaction.setDescription("Test transaction");
        return transaction;
    }




    public static AuthRequest createAuthRequest(String firstName, String lastName, String password) {
        AuthRequest request = new AuthRequest();
        request.setFirstName(firstName);
        request.setLastName(lastName);
        request.setPassword(password);
        return request;
    }



    public static AuthResponse createAuthResponse(String token, User user) {
        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setUserId(user.getId());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        return response;
    }


}