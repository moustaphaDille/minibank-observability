package org.moustapha.accountservice.model;
import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
public class Account {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long customerId;

    @Column(precision = 19, scale = 2)
    private BigDecimal balance;

    protected Account() {}

    public Account(Long customerId, BigDecimal balance) {
        this.customerId = customerId;
        this.balance = balance;
    }

    public Long getId() { return id; }
    public Long getCustomerId() { return customerId; }
    public BigDecimal getBalance() { return balance; }

    public void credit(BigDecimal amount) {
        this.balance = this.balance.add(amount);
    }

    public void debit(BigDecimal amount) {
        this.balance = this.balance.subtract(amount);
    }
}
