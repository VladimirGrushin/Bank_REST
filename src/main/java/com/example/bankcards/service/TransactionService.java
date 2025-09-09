package com.example.bankcards.service;
import com.example.bankcards.entity.BankCard;
import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.User;
import com.example.bankcards.exception.CardOperationException;
import com.example.bankcards.exception.InsufficientFundsException;
import com.example.bankcards.exception.ResourceNotFoundException;
import com.example.bankcards.repository.BankCardRepository;
import com.example.bankcards.repository.TransactionRepository;
import com.example.bankcards.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final BankCardRepository bankCardRepository;
    private final UserRepository userRepository;

    private User getCurrentUser() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByFirstNameAndLastName(username.split(" ")[0], username.split(" ")[1])
                .orElseThrow(() -> new ResourceNotFoundException("User", "firstName",  username.split(" ")[0], "lastName", username.split(" ")[1]));
    }

    // Перевод между своими картами
    public Transaction transferBetweenMyCards(Long fromCardId, Long toCardId, BigDecimal value, String description){
        User currentUser = getCurrentUser();
        BankCard fromCard = bankCardRepository.findByIdAndOwnerId(fromCardId, currentUser.getId())
                 .orElseThrow(() -> new AccessDeniedException("Source card not found or access denied"));
        BankCard toCard = bankCardRepository.findByIdAndOwnerId(toCardId, currentUser.getId())
                .orElseThrow(() -> new AccessDeniedException("Destination card not found or access denied"));

        if (!fromCard.isActive()) throw new CardOperationException("Source card is not active");
        if (!toCard.isActive()) throw new CardOperationException("Destination card is not active");
        if (fromCard.getBalance().compareTo(value) < 0) throw new InsufficientFundsException("Insufficient funds on source card. Available: " + fromCard.getBalance());

        fromCard.withdraw(value);
        toCard.deposit(value);
        bankCardRepository.save(fromCard);
        bankCardRepository.save(toCard);

        Transaction transaction = new Transaction();
        transaction.setAmount(value);
        transaction.setToCard(toCard);
        transaction.setFromCard(fromCard);
        transaction.setDescription(description);
        return transactionRepository.save(transaction);
    }

    // Получить список транзакций текущего пользователя
    public List<Transaction> muTransactions(){
        User currentUser = getCurrentUser();
        return transactionRepository.findByFromCardOwnerIdOrToCardOwnerId(currentUser.getId(), currentUser.getId());
    }


}
