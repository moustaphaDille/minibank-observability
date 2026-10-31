package org.moustapha.accountservice.controller;
import jakarta.validation.Valid;
import org.moustapha.accountservice.dto.AccountRequest;
import org.moustapha.accountservice.dto.AccountResponse;
import org.moustapha.accountservice.dto.AmountRequest;
import org.moustapha.accountservice.service.AccountService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/accounts")
public class AccountController {

    private final AccountService service;

    public AccountController(AccountService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AccountResponse create(@Valid @RequestBody AccountRequest request) {
        return service.create(request);
    }

    @GetMapping
    public List<AccountResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public AccountResponse findById(@PathVariable Long id) {
        return service.findById(id);
    }

    @PostMapping("/{id}/credit")
    public AccountResponse credit(@PathVariable Long id, @Valid @RequestBody AmountRequest request) {
        return service.credit(id, request.amount());
    }

    @PostMapping("/{id}/debit")
    public AccountResponse debit(@PathVariable Long id, @Valid @RequestBody AmountRequest request) {
        return service.debit(id, request.amount());
    }
}
