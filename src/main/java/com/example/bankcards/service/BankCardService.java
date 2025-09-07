package com.example.bankcards.service;


import com.example.bankcards.entity.BankCard;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.entity.User;
import com.example.bankcards.repository.BankCardRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;

import javax.transaction.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class BankCardService {
    private final BankCardRepository bankCardRepository;
    private final UserRepository userRepository;

    @Value("${encryption.secret-key:defaultSecretKey}") // Значение по умолчанию
    private String secretKey;

    // === МЕТОДЫ ШИФРОВАНИЯ  ===

    private String encrypt(String data) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, keySpec);
            byte[] encrypted = cipher.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            throw new RuntimeException("Encryption failed", e);
        }
    }

    private String decrypt(String encryptedData) {
        try {
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            SecretKeySpec keySpec = new SecretKeySpec(secretKey.getBytes(StandardCharsets.UTF_8), "AES");
            cipher.init(Cipher.DECRYPT_MODE, keySpec);
            byte[] decoded = Base64.getDecoder().decode(encryptedData);
            byte[] decrypted = cipher.doFinal(decoded);
            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Decryption failed", e);
        }
    }

    // Получить текущего пользователя
    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByFirstNameAndLastName(username.split(" ")[0], username.split(" ")[1])
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public void isUserAdmin() {
        User currentUser = getCurrentUser();
        if (!currentUser.isAdmin()) {
            throw new AccessDeniedException("Only administrators can perform this action");
        }
    }

    // ==== МЕТОДЫ АДМИНА ====

    public BankCard createNewCard(String cardNumber, String cardOwnerName, Long ownerId){
        isUserAdmin();
        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        BankCard newCard = new BankCard(encrypt(cardNumber), cardOwnerName,LocalDate.now(), user);
        newCard.setStatus(CardStatus.ACTIVE);
        newCard.setBalance(BigDecimal.ZERO);
        return  bankCardRepository.save(newCard);
    }

    // Админ получает незамаскированный номер карты
    public String getCardNumberForAdmin(Long cardId){
        isUserAdmin();
        BankCard card = bankCardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("No card with such id"));
        return decrypt(card.getCardNumber());
    }

    public BankCard activateCardByAdmin(Long id){
        isUserAdmin();
        BankCard card = bankCardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No card with such id"));
        if (card.isExpired())throw new RuntimeException("Can not activate expired card");
        card.activateCard();
        return bankCardRepository.save(card);
    }

    public void deleteCardByAdmin(Long id){
        isUserAdmin();
        BankCard card = bankCardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No card with such id"));
        if (card.getBalance().compareTo(BigDecimal.ZERO) != 0) throw new RuntimeException("Can not block card with non-zero balance");
        bankCardRepository.delete(card);
    }

    // Админ подтверждает запрос на блокировку от пользователя
    public BankCard approveBlockRequest(Long id, String reason){
        isUserAdmin();
        BankCard card = bankCardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No card with such id"));
        if (!card.isBlockRequested()) throw new RuntimeException("No block request pending for this card");
        card.approveBlockRequest(reason);
        return bankCardRepository.save(card);
    }

    // Админ отклоняет запрос на блокировку от пользователя
    public BankCard rejectBlockRequest(Long id){
        isUserAdmin();
        BankCard card = bankCardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No card with such id"));
        if (!card.isBlockRequested()) throw new RuntimeException("No block request pending for this card");
        card.rejectBlockRequest();
        return bankCardRepository.save(card);
    }

    // Админ блокирует карту самостоятельно
    public BankCard blockCard(Long id, String reason){
        isUserAdmin();
        BankCard card = bankCardRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("No card with such id"));
        if (card.isBlocked()) throw new RuntimeException("Card is already blocked");
        card.blockCard(reason);
        return bankCardRepository.save(card);
    }

    // Получить все карты по статусу
    public List<BankCard> getCardsByStatus(CardStatus status) {
        isUserAdmin();
        return bankCardRepository.findByStatus(status);
    }

    // Получить все карты в системе
    public List<BankCard> getAllCards() {
        isUserAdmin();
        return bankCardRepository.findAll();
    }

    // === ОБЩИЕ МЕТОДЫ ===

    // Пользователи получают свою карту с маскированным номером
    public BankCard getCardById(Long cardId){
        User currentUser = getCurrentUser();
        BankCard card = bankCardRepository.findById(cardId)
                .orElseThrow(() -> new RuntimeException("No card with such id"));
        if (!currentUser.isAdmin() && !card.getOwner().getId().equals(currentUser.getId())) throw new AccessDeniedException("Access denied");
        if(!currentUser.isAdmin()) {
            String decryptedNumber = decrypt(card.getCardNumber());
            String maskNumber = "**** **** ****" + decryptedNumber.substring(decryptedNumber.length() - 4);
            card.setMaskedCardNumber(maskNumber);
            card.setCardNumber(null);
        }
        return card;
    }

    public List<BankCard> getMyCards(){
        User currentUser = getCurrentUser();
        List<BankCard> cards = bankCardRepository.findByOwnerId(currentUser.getId());
        for (BankCard card : cards){
            String decryptNumber = decrypt(card.getCardNumber());
            String maskNumber = "**** **** ****" + decryptNumber.substring(decryptNumber.length() - 4);
            card.setMaskedCardNumber(maskNumber);
            card.setCardNumber(null);
        }
        return cards;
    }

    // Запрос на блокировку карты
    public BankCard requestBlockCard(Long id, String reason){
        User currentUser = getCurrentUser();
        BankCard card = bankCardRepository.findByIdAndOwnerId(id, currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Card not found or access denied"));
        if (card.isBlocked()) throw new RuntimeException("Card is already blocked");
        if (card.isBlockRequested()) throw new RuntimeException("Block request already pending");
        card.requestBlock(reason);
        return bankCardRepository.save(card);
    }

    // Отменить запрос на блокировку
    public BankCard cancelRequestBlockCard(Long id){
        User currentUser = getCurrentUser();
        BankCard card = bankCardRepository.findByIdAndOwnerId(id, currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Card not found or access denied"));
        if (!card.isBlockRequested()) throw new RuntimeException("There is now block request");
        card.rejectBlockRequest();
        return bankCardRepository.save(card);
    }


    // Просмотр баланса
    public BigDecimal getCardBalance(Long cardId){
        User currentUser = getCurrentUser();
        BankCard card = bankCardRepository.findByIdAndOwnerId(cardId, currentUser.getId())
                .orElseThrow(() -> new RuntimeException("Card not found or access denied"));
        return card.getBalance();
    }

}