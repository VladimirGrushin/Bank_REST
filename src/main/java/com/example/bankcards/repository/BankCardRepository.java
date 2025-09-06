package com.example.bankcards.repository;


import com.example.bankcards.entity.BankCard;
import com.example.bankcards.entity.CardStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BankCardRepository extends JpaRepository<BankCard, Long> {

    // Найти все карты пользователя
    List<BankCard> findByOwnerId(Long ownerId);

    // Найти все карты пользователя с пагинацией
    Page<BankCard> findByOwnerId(Long ownerId, Pageable pageable);

    // Найти конкретную карту пользователя
    Optional<BankCard> findByIdAndOwnerId(Long id, Long ownerId);

    // Найти карты по статусу
    List<BankCard> findByStatus(CardStatus status);

    // Найти карты с истёкшим сроком действия
    List<BankCard> findByExpirationDateBetween(LocalDate start, LocalDate end);


    // Поиск карт пользователя по имени владельца карты
    @Query("SELECT c FROM BankCard c WHERE c.owner.id = :userId AND " +
            "LOWER(c.cardHolderName) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<BankCard> findByOwnerIdAndCardHolderNameContaining(
            @Param("userId") Long userId,
            @Param("search") String search,
            Pageable pageable);

}