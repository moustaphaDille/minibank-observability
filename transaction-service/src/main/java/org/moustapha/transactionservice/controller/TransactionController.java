package org.moustapha.transactionservice.controller;

import jakarta.validation.Valid;
import org.moustapha.transactionservice.dto.TransactionResponse;
import org.moustapha.transactionservice.dto.TransferRequest;
import org.moustapha.transactionservice.service.TransactionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service) {
        this.service = service;
    }

    @PostMapping("/transfer")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionResponse transfer(@Valid @RequestBody TransferRequest request) {
        return service.transfer(request);
    }

    @GetMapping
    public List<TransactionResponse> findAll() {
        return service.findAll();
    }
}
