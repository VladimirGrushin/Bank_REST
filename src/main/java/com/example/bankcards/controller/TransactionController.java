package com.example.bankcards.controller;

import com.example.bankcards.entity.Transaction;
import com.example.bankcards.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {
    private final TransactionService transactionService;

    // Перевод между своими картами
    @PostMapping("/transfer/my-cards")
    public ResponseEntity<Transaction> transferBetweenMyCards(@RequestParam Long fromCardId, @RequestParam Long toCardId, @RequestParam BigDecimal amount, @RequestParam(required = false) String description) {
        Transaction transaction = transactionService.transferBetweenMyCards(fromCardId, toCardId, amount, description);
        return ResponseEntity.ok(transaction);
    }

    // Получить историю транзакций текущего пользователя
    @GetMapping("/my")
    public ResponseEntity<List<Transaction>> getMyTransactions() {
        List<Transaction> transactions = transactionService.muTransactions();
        return ResponseEntity.ok(transactions);
    }
}
