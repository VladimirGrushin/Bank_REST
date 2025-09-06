package com.example.bankcards.repository;

import com.example.bankcards.entity.User;
import com.example.bankcards.entity.Role;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // Базовые методы поиска
    Optional<User> findByFirstName(String firstName);
    Optional<User> findByLastName(String lastName);

    // Поиск по комбинации имени и фамилии
    Optional<User> findByFirstNameAndLastName(String firstName, String lastName);

    // Поиск по роли
    List<User> findByRole(Role role);
    Page<User> findByRole(Role role, Pageable pageable);

    // Проверки существования
    Boolean existsByFirstName(String firstName);
    Boolean existsByLastName(String lastName);
    Boolean existsByFirstNameAndLastName(String firstName, String lastName);

    // Кастомные запросы для сложного поиска
    @Query("SELECT u FROM User u WHERE " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(u.lastName) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    Page<User> searchByName(@Param("searchTerm") String searchTerm, Pageable pageable);


    // Поиск пользователей с картами
    @Query("SELECT u FROM User u WHERE SIZE(u.cards) > 0")
    List<User> findUsersWithCards();

    @Query("SELECT u FROM User u WHERE SIZE(u.cards) = 0")
    List<User> findUsersWithoutCards();


    // Для административных функций
    @Query("SELECT u FROM User u ORDER BY u.firstName, u.lastName")
    List<User> findAllOrderByName();

}