package com.example.bankcards.entity;

import javax.persistence.*;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "bank_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"owner", "outgoingTransactions", "incomingTransactions"})
public class BankCard {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String blockReason; // Причина блокировки
    private Boolean blockRequested = false; // Флаг запроса на блокировку
    private String blockRequestReason;  // Причина запроса блокировки

    @Column(name = "card_number", nullable = false, length = 16, unique = true)
    @NotBlank(message = "Card number is required")
    @Pattern(regexp = "^[0-9]{16}$", message = "Card number must contain 16 digits")
    private String cardNumber;

    @Column(name = "card_owner_name", nullable = false, length = 100)
    @NotBlank(message = "Card owner name is required")
    @Size(max = 100, message = "Card holder name must not exceed 100 characters")
    private String cardOwnerName;

    @Column(name = "validity_period", nullable = false, length = 25)
    @Future(message = "Validity period must be in the future")
    @NotNull(message = "Validity period is required")
    private LocalDate validityPeriod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CardStatus status = CardStatus.ACTIVE;

    @Column(nullable = false, precision = 19, scale = 2)
    @DecimalMin(value = "0.00", message = "Balance cannot be negative")
    @Digits(integer = 16, fraction = 2, message = "Balance format is invalid")
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_card_user"))
    @NotNull(message = "Card owner is required")
    private User owner;

    @OneToMany(mappedBy = "fromCard", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> outgoingTransactions = new ArrayList<>();

    @OneToMany(mappedBy = "toCard", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Transaction> incomingTransactions = new ArrayList<>();

    @Transient
    private String maskedCardNumber;

    @PostLoad
    @PostPersist
    @PostUpdate
    // Маскируем номер карты, оставляя только последние 4 цифры
    private void maskCardNumber(){
        if (cardNumber != null && cardNumber.length() >=4){
            String lastFour = cardNumber.substring(cardNumber.length() - 4);
            cardNumber = "**** **** **** " + lastFour;
        }else cardNumber = "**** **** **** ****";
    }


    @PrePersist
    protected void onCreate(){
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        checkAndUpdateExpiredStatus();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
        checkAndUpdateExpiredStatus(); // Проверяем при обновлении
    }


    //Обновляем статус карты если она просрочена
    public void checkAndUpdateExpiredStatus(){
        if (isExpired() && status != CardStatus.EXPIRED) status = CardStatus.EXPIRED;
    }

    // Проверяем просрочена ли карта
    public boolean isExpired(){
        return LocalDate.now().isAfter(validityPeriod) || LocalDate.now().equals(validityPeriod);
    }

    // Проверяем активна ли карта ( не заблокирована и не просрочена)
    public boolean isActive(){
        return status == CardStatus.ACTIVE && !isExpired() && !isBlocked();
    }

    // Проверяем заблокирована ли карта
    public boolean isBlocked(){
        return status == CardStatus.BLOCKED;
    }

    // Пополняем баланс карты
    public void deposit(BigDecimal value){
        if (value.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Deposit must be positive");
        this.balance = this.balance.add(value);
    }

    // Снимаем деньги с карты
    public void withdraw(BigDecimal value){
        if (value.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("Withdrawing value must be positive");
        if (this.balance.compareTo(value) < 0) throw new IllegalArgumentException("The withdrawal amount is greater than the balance");
        if (!isActive()) throw new IllegalStateException("Card is not active");
        this.balance = this.balance.subtract(value);
    }


    // Блокировка карты
    public void blockCard(String reason){
        status = CardStatus.BLOCKED;
        this.blockReason = reason;
        this.blockRequested = false; // Сбрасываем запрос после блокировки
        this.blockRequestReason = null;
    }

    // Активация карты
    public void activateCard(){
        if (isExpired()) throw new IllegalStateException("Expired card can not not be activated");
        status = CardStatus.ACTIVE;
        this.blockReason = null;
        this.blockRequested = false;
        this.blockRequestReason = null;
    }

    // Запрос на блокировку
    public void requestBlock(String reason) {
        if (this.status != CardStatus.ACTIVE) throw new IllegalStateException("Only active cards can request block");
        this.blockRequested = true;
        this.blockRequestReason = reason;
    }

    // Админ подтверждает запрос на блокировку от пользователя
    public void approveBlockRequest(String adminReason) {
        if (!this.blockRequested) throw new IllegalStateException("No block request pending");
        this.status = CardStatus.BLOCKED;
        this.blockReason = adminReason != null ? adminReason : this.blockRequestReason;
        this.blockRequested = false;
        this.blockRequestReason = null;
    }

    // Админ отклоняет запрос на блокировку
    public void rejectBlockRequest() {
        if (!this.blockRequested) throw new IllegalStateException("No block request pending");
        this.blockRequested = false;
        this.blockRequestReason = null;
    }

    public boolean isBlockRequested() {
        return Boolean.TRUE.equals(this.blockRequested);
    }

    // Получить последние 4 цифры номера карты
    public String getFourDigits(){
        if (cardNumber != null && cardNumber.length() >= 4)return cardNumber.substring(cardNumber.length() - 4);
        return "****";
    }

    // Проверка владельца карты
    public boolean isOwnedBy(User user){
        return this.owner.getId().equals(user.getId());
    }

    @Override
    public boolean equals(Object o){
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BankCard bankCard = (BankCard) o;
        return id != null && id.equals(bankCard.id);
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    public BankCard(String cardNumber, String cardOwnerName, LocalDate validityPeriod, User owner){
        this.cardOwnerName = cardOwnerName;
        this.cardNumber = cardNumber;
        this.validityPeriod = validityPeriod;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.owner = owner;
    }
}