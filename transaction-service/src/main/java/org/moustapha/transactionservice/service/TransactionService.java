package org.moustapha.transactionservice.service;

import feign.FeignException;
import feign.RetryableException;
import org.moustapha.transactionservice.client.AccountClient;
import org.moustapha.transactionservice.dto.AmountDto;
import org.moustapha.transactionservice.dto.TransactionResponse;
import org.moustapha.transactionservice.dto.TransferRequest;
import org.moustapha.transactionservice.model.Transaction;
import org.moustapha.transactionservice.model.TransactionStatus;
import org.moustapha.transactionservice.repository.TransactionRepository;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import java.util.List;


@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private final TransactionRepository repository;
    private final AccountClient accountClient;

    public TransactionService(TransactionRepository repository, AccountClient accountClient) {
        this.repository = repository;
        this.accountClient = accountClient;
    }

    public TransactionResponse transfer(TransferRequest request) {
        if (request.sourceAccountId().equals(request.targetAccountId())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Les deux comptes doivent être différents");
        }

        AmountDto amount = new AmountDto(request.amount());

        try {
            // Étape 1 : débiter la source
            accountClient.debit(request.sourceAccountId(), amount);

            // Étape 2 : créditer la cible
            try {
                accountClient.credit(request.targetAccountId(), amount);
            } catch (FeignException e) { // couvre aussi RetryableException
                // Le débit est déjà fait : on rembourse la source
                compensate(request, amount);
                throw e;
            }

            Transaction saved = repository.save(new Transaction(
                    request.sourceAccountId(), request.targetAccountId(),
                    request.amount(), TransactionStatus.COMPLETED));
            return TransactionResponse.from(saved);

        } catch (FeignException.NotFound e) {
            saveFailed(request);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte introuvable");
        } catch (FeignException.UnprocessableEntity e) {
            saveFailed(request);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Solde insuffisant");
        } catch (RetryableException e) {
            saveFailed(request);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "account-service indisponible");
        }
    }

    public List<TransactionResponse> findAll() {
        return repository.findAll().stream().map(TransactionResponse::from).toList();
    }

    private void compensate(TransferRequest request, AmountDto amount) {
        try {
            accountClient.credit(request.sourceAccountId(), amount);
            log.warn("Crédit cible échoué : source {} remboursée", request.sourceAccountId());
        } catch (Exception ex) {
            log.error("COMPENSATION ÉCHOUÉE : le compte {} a été débité de {} sans crédit de la cible",
                    request.sourceAccountId(), request.amount());
        }
    }

    private void saveFailed(TransferRequest request) {
        repository.save(new Transaction(
                request.sourceAccountId(), request.targetAccountId(),
                request.amount(), TransactionStatus.FAILED));
    }
}
