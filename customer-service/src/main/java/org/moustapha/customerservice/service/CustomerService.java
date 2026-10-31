package org.moustapha.customerservice.service;


import org.moustapha.customerservice.dto.CustomerRequest;
import org.moustapha.customerservice.dto.CustomerResponse;
import org.moustapha.customerservice.model.Customer;
import org.moustapha.customerservice.repository.CustomerRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CustomerService {

    private final CustomerRepository repository;

    public CustomerService(CustomerRepository repository) {
        this.repository = repository;
    }

    public CustomerResponse create(CustomerRequest request) {
        if (repository.existsByEmail(request.email())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email déjà utilisé");
        }
        Customer saved = repository.save(
                new Customer(request.firstName(), request.lastName(), request.email()));
        return CustomerResponse.from(saved);
    }

    public List<CustomerResponse> findAll() {
        return repository.findAll().stream().map(CustomerResponse::from).toList();
    }

    public CustomerResponse findById(Long id) {
        return repository.findById(id)
                .map(CustomerResponse::from)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Client introuvable : " + id));
    }
}
