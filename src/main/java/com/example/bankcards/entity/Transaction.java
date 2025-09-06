package com.example.bankcards.entity;
import javax.persistence.*;
import javax.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.example.bankcards.entity.TransactionStatus;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;

@Entity
@Table(name = "Transactions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(exclude = {"fromCard", "toCard"})

public class Transaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_card_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transaction_from_card"))
    @NotNull(message = "Source card is required")
    private BankCard fromCard;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_card_id", nullable = false, foreignKey = @ForeignKey(name = "fk_transaction_to_card"))
    @NotNull(message = "Destination card is required")
    private BankCard toCard;

    @Column(nullable = false, precision = 19, scale = 2)
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 16, fraction = 2, message = "Invalid amount format")
    private BigDecimal amount;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(length = 255)
    @Size(max = 255, message = "Description message too long")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status = TransactionStatus.PENDING;

    @PrePersist
    protected void onCreate() {
        if (timestamp == null) timestamp = LocalDateTime.now();
    }

    public boolean isSuccessful() {
        return this.status == TransactionStatus.SUCCESS;
    }

    public boolean isFailed() {
        return this.status == TransactionStatus.FAILED;
    }

    public boolean isPending() {
        return this.status == TransactionStatus.PENDING;
    }

    public boolean involvesCard(Long cardId) {
        return fromCard.getId().equals(cardId) || toCard.getId().equals(cardId);
    }

    public boolean isOwnedBy(User user) {
        return fromCard.isOwnedBy(user) || toCard.isOwnedBy(user);
    }
}
