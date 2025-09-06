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

    private BankCard createNewCard(String cardNumber, String cardOwnerName, Long ownerId){
        isUserAdmin();
        User user = userRepository.findById(ownerId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        BankCard newCard = new BankCard(encrypt(cardNumber), cardOwnerName,LocalDate.now(), user);
        newCard.setStatus(CardStatus.ACTIVE);
        newCard.setBalance(BigDecimal.ZERO);
        return  bankCardRepository.save(newCard);
    }






}