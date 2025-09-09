package com.example.bankcards.controller;


import com.example.bankcards.dto.response.BankCardResponse;
import com.example.bankcards.entity.BankCard;
import com.example.bankcards.entity.CardStatus;
import com.example.bankcards.service.BankCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;


@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class BankCardController {
    private final BankCardService bankCardService;

    @GetMapping("/my")
    public ResponseEntity<List<BankCard>> getMyCards(){
        List<BankCard> cards = bankCardService.getMyCards();
        return ResponseEntity.ok(cards);
    }

    @PostMapping("/admin/create")
    public ResponseEntity<BankCard> createCard(@RequestParam String cardNumber, @RequestParam String cardOwnerName, @RequestParam Long ownerId){
        BankCard card = bankCardService.createNewCard(cardNumber, cardOwnerName, ownerId);
        return ResponseEntity.ok(card);
    }

    @GetMapping("/admin/{cardId}/number")
    public ResponseEntity<String> getCardNumberForAdmin(@PathVariable Long cardId) {
        String cardNumber = bankCardService.getCardNumberForAdmin(cardId);
        return ResponseEntity.ok(cardNumber);
    }

    @PatchMapping("/admin/{cardId}/activate")
    public ResponseEntity<BankCard> activateCard(@PathVariable Long cardId){
        BankCard card = bankCardService.activateCardByAdmin(cardId);
        return ResponseEntity.ok(card);
    }

    @DeleteMapping("/admin/{cardId}")
    public ResponseEntity<Void> deleteCard(@PathVariable Long cardId){
        bankCardService.deleteCardByAdmin(cardId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/admin/{cardId}/approve-block")
    public ResponseEntity<BankCard> approveBlockRequest(@PathVariable Long cardId, @RequestParam String reason){
        BankCard card = bankCardService.approveBlockRequest(cardId, reason);
        return ResponseEntity.ok(card);
    }

    @PatchMapping("/admin/{cardId}/reject-block")
    public ResponseEntity<BankCard> rejectBlockRequest(@PathVariable Long cardId){
        BankCard card = bankCardService.rejectBlockRequest(cardId);
        return ResponseEntity.ok(card);
    }

    @PatchMapping("/admin/{cardId}/block")
    public ResponseEntity<BankCard> blockCard(@PathVariable Long cardId, @RequestParam String reason) {
        BankCard card = bankCardService.blockCard(cardId, reason);
        return ResponseEntity.ok(card);
    }


    @PatchMapping("/{cardId}/request-block")
    public ResponseEntity<BankCard> requestBlockCard(@PathVariable Long cardId, @RequestParam String reason) {
        BankCard card = bankCardService.requestBlockCard(cardId, reason);
        return ResponseEntity.ok(card);
    }


    @PatchMapping("/{cardId}/cancel-block-request")
    public ResponseEntity<BankCard> cancelBlockRequest(@PathVariable Long cardId) {
        BankCard card = bankCardService.cancelRequestBlockCard(cardId);
        return ResponseEntity.ok(card);
    }


    @GetMapping("/{cardId}/balance")
    public ResponseEntity<BigDecimal> getCardBalance(@PathVariable Long cardId) {
        BigDecimal balance = bankCardService.getCardBalance(cardId);
        return ResponseEntity.ok(balance);
    }

    // Получить все карты по статусу
    @GetMapping("/admin/status/{status}")
    public ResponseEntity<List<BankCard>> getCardsByStatus(@PathVariable CardStatus status) {
        List<BankCard> cards = bankCardService.getCardsByStatus(status);
        return ResponseEntity.ok(cards);
    }

    // Получить все карты
    @GetMapping("/admin/all")
    public ResponseEntity<List<BankCard>> getAllCards() {
        List<BankCard> cards = bankCardService.getAllCards();
        return ResponseEntity.ok(cards);
    }

    @GetMapping("/{cardId}")
    public ResponseEntity<BankCard> getCardById(@PathVariable Long cardId){
        BankCard card = bankCardService.getCardById(cardId);
        return ResponseEntity.ok(card);
    }
}
