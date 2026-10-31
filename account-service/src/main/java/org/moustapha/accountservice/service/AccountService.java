package org.moustapha.accountservice.service;
import feign.FeignException;
import feign.RetryableException;
import org.moustapha.accountservice.client.CustomerClient;
import org.moustapha.accountservice.dto.AccountRequest;
import org.moustapha.accountservice.dto.AccountResponse;
import org.moustapha.accountservice.model.Account;
import org.moustapha.accountservice.repository.AccountRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;

@Service
public class AccountService {

    private final AccountRepository repository;
    private final CustomerClient customerClient;

    public AccountService(AccountRepository repository, CustomerClient customerClient) {
        this.repository = repository;
        this.customerClient = customerClient;
    }

    public AccountResponse create(AccountRequest request) {
        checkCustomerExists(request.customerId());
        Account saved = repository.save(
                new Account(request.customerId(), request.initialBalance()));
        return AccountResponse.from(saved);
    }

    private void checkCustomerExists(Long customerId) {
        try {
            customerClient.getById(customerId);
        } catch (FeignException.NotFound e) {
            // customer-service répond, mais le client n'existe pas
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Client introuvable : " + customerId);
        } catch (RetryableException e) {
            // impossible de joindre customer-service (arrêté, réseau coupé...)
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "customer-service indisponible");
        }
    }

    public List<AccountResponse> findAll() {
        return repository.findAll().stream().map(AccountResponse::from).toList();
    }

    public AccountResponse findById(Long id) {
        return AccountResponse.from(getOrThrow(id));
    }

    @Transactional
    public AccountResponse credit(Long id, BigDecimal amount) {
        Account account = getOrThrow(id);
        account.credit(amount);
        return AccountResponse.from(account);
    }

    @Transactional
    public AccountResponse debit(Long id, BigDecimal amount) {
        Account account = getOrThrow(id);
        if (account.getBalance().compareTo(amount) < 0) {
            throw new ResponseStatusException(
                    HttpStatus.UNPROCESSABLE_ENTITY, "Solde insuffisant");
        }
        account.debit(amount);
        return AccountResponse.from(account);
    }

    private Account getOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Compte introuvable : " + id));
    }
}
