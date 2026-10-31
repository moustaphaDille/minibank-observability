package org.moustapha.customerservice.dto;
import org.moustapha.customerservice.model.Customer;

public record CustomerResponse(Long id, String firstName, String lastName, String email) {

    public static CustomerResponse from(Customer c) {
        return new CustomerResponse(c.getId(), c.getFirstName(), c.getLastName(), c.getEmail());
    }
}
