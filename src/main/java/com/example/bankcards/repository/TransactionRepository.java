package com.example.bankcards.repository;


import com.example.bankcards.entity.Transaction;
import com.example.bankcards.entity.TransactionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    // Найти транзакции по карте отправителя
    List<Transaction> findByFromCardId(Long fromCardId);

    // Найти транзакции по карте получателя
    List<Transaction> findByToCardId(Long toCardId);

    // Найти транзакции по карте (отправитель или получатель). Данный метод оказался не нужен, так как по ТЗ необходимо реализовать переводы только между своими картами
    List<Transaction> findByFromCardIdOrToCardId(Long fromCardId, Long toCardId);

    //Найти транзакции по id получателя и отправителя. Тут тоже можно было ограничиться только одним id, так как переводы только между своими картами
    List<Transaction> findByFromCardOwnerIdOrToCardOwnerId(Long fromCardOwnerId, Long toCardOwnerId);

    // Найти транзакции пользователя (где он отправитель или получатель)
    @Query("SELECT t FROM Transaction t WHERE " +
            "t.fromCard.owner.id = :userId OR t.toCard.owner.id = :userId")
    Page<Transaction> findByUserId(@Param("userId") Long userId, Pageable pageable);

    // Найти транзакции по статусу
    List<Transaction> findByStatus(TransactionStatus status);

    // Найти транзакции за период времени
    List<Transaction> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

}