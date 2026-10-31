package org.moustapha.transactionservice.client;

import org.moustapha.transactionservice.dto.AccountDto;
import org.moustapha.transactionservice.dto.AmountDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "account-service", url = "${services.account.url}")
public interface AccountClient {

    @PostMapping("/api/accounts/{id}/debit")
    AccountDto debit(@PathVariable("id") Long id, @RequestBody AmountDto body);

    @PostMapping("/api/accounts/{id}/credit")
    AccountDto credit(@PathVariable("id") Long id, @RequestBody AmountDto body);
}
