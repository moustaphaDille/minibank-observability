package org.moustapha.transactionservice.service;
import feign.FeignException;
import feign.RetryableException;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.moustapha.transactionservice.client.AccountClient;
import org.moustapha.transactionservice.dto.AmountDto;
import org.moustapha.transactionservice.dto.TransactionResponse;
import org.moustapha.transactionservice.dto.TransferRequest;
import org.moustapha.transactionservice.model.Transaction;
import org.moustapha.transactionservice.model.TransactionStatus;
import org.moustapha.transactionservice.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private static final String TRANSFERS = "minibank.transfers";
    private static final List<String> FAILURE_REASONS = List.of(
            "same_account", "account_not_found", "insufficient_funds",
            "service_unavailable", "account_error");

    private final TransactionRepository repository;
    private final AccountClient accountClient;
    private final MeterRegistry meterRegistry;

    private final Counter transfersCompleted;
    private final Counter compensationFailures;
    private final DistributionSummary transferAmount;

    public TransactionService(TransactionRepository repository,
                              AccountClient accountClient,
                              MeterRegistry meterRegistry) {
        this.repository = repository;
        this.accountClient = accountClient;
        this.meterRegistry = meterRegistry;

        // On crée les compteurs au démarrage : ils existent dès le départ, à la valeur 0
        this.transfersCompleted = Counter.builder(TRANSFERS)
                .description("Nombre de virements par résultat")
                .tag("status", "completed")
                .tag("reason", "none")
                .register(meterRegistry);

        FAILURE_REASONS.forEach(this::failureCounter);

        this.compensationFailures = Counter.builder("minibank.compensation.failures")
                .description("Compensations échouées : compte débité sans crédit de la cible")
                .register(meterRegistry);

        this.transferAmount = DistributionSummary.builder("minibank.transfer.amount")
                .description("Montants des virements réussis")
                .register(meterRegistry);
    }

    public TransactionResponse transfer(TransferRequest request) {
        log.info("Virement demandé : compte {} vers compte {}, montant {}",
                request.sourceAccountId(), request.targetAccountId(), request.amount());

        if (request.sourceAccountId().equals(request.targetAccountId())) {
            log.warn("Virement refusé : les deux comptes sont identiques ({})", request.sourceAccountId());
            failureCounter("same_account").increment();
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Les deux comptes doivent être différents");
        }

        AmountDto amount = new AmountDto(request.amount());

        try {
            // Étape 1 : débiter la source
            accountClient.debit(request.sourceAccountId(), amount);
            log.info("Débit effectué sur le compte source {}", request.sourceAccountId());

            // Étape 2 : créditer la cible
            try {
                accountClient.credit(request.targetAccountId(), amount);
                log.info("Crédit effectué sur le compte cible {}", request.targetAccountId());
            } catch (FeignException e) { // couvre aussi RetryableException
                log.error("Crédit impossible sur le compte {} : {}", request.targetAccountId(), e.getMessage());
                compensate(request, amount);
                throw e;
            }

            Transaction saved = repository.save(new Transaction(
                    request.sourceAccountId(), request.targetAccountId(),
                    request.amount(), TransactionStatus.COMPLETED));
            log.info("Virement {} terminé avec succès", saved.getId());

            transfersCompleted.increment();
            transferAmount.record(request.amount().doubleValue());
            return TransactionResponse.from(saved);

        } catch (FeignException.NotFound e) {
            log.warn("Virement échoué : compte introuvable");
            failureCounter("account_not_found").increment();
            saveFailed(request);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte introuvable");
        } catch (FeignException.UnprocessableEntity e) {
            log.warn("Virement échoué : solde insuffisant sur le compte {}", request.sourceAccountId());
            failureCounter("insufficient_funds").increment();
            saveFailed(request);
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Solde insuffisant");
        } catch (RetryableException e) {
            log.error("Virement échoué : account-service injoignable ({})", e.getMessage());
            failureCounter("service_unavailable").increment();
            saveFailed(request);
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE, "account-service indisponible");
        } catch (FeignException e) {
            log.error("Virement échoué : erreur inattendue d'account-service ({})", e.getMessage());
            failureCounter("account_error").increment();
            throw e;
        }
    }

    public List<TransactionResponse> findAll() {
        return repository.findAll().stream().map(TransactionResponse::from).toList();
    }

    private Counter failureCounter(String reason) {
        return Counter.builder(TRANSFERS)
                .description("Nombre de virements par résultat")
                .tag("status", "failed")
                .tag("reason", reason)
                .register(meterRegistry);
    }

    private void compensate(TransferRequest request, AmountDto amount) {
        try {
            accountClient.credit(request.sourceAccountId(), amount);
            log.warn("Crédit cible échoué : source {} remboursée", request.sourceAccountId());
        } catch (Exception ex) {
            compensationFailures.increment();
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


//package org.moustapha.transactionservice.service;
//import feign.FeignException;
//import feign.RetryableException;
//import org.moustapha.transactionservice.client.AccountClient;
//import org.moustapha.transactionservice.dto.AmountDto;
//import org.moustapha.transactionservice.dto.TransactionResponse;
//import org.moustapha.transactionservice.dto.TransferRequest;
//import org.moustapha.transactionservice.model.Transaction;
//import org.moustapha.transactionservice.model.TransactionStatus;
//import org.moustapha.transactionservice.repository.TransactionRepository;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//import org.springframework.web.server.ResponseStatusException;
//
//import java.util.List;
//
//@Service
//public class TransactionService {
//
//    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
//
//    private final TransactionRepository repository;
//    private final AccountClient accountClient;
//
//    public TransactionService(TransactionRepository repository, AccountClient accountClient) {
//        this.repository = repository;
//        this.accountClient = accountClient;
//    }
//
//    public TransactionResponse transfer(TransferRequest request) {
//        log.info("Virement demandé : compte {} vers compte {}, montant {}",
//                request.sourceAccountId(), request.targetAccountId(), request.amount());
//
//        if (request.sourceAccountId().equals(request.targetAccountId())) {
//            log.warn("Virement refusé : les deux comptes sont identiques ({})", request.sourceAccountId());
//            throw new ResponseStatusException(
//                    HttpStatus.BAD_REQUEST, "Les deux comptes doivent être différents");
//        }
//
//        AmountDto amount = new AmountDto(request.amount());
//
//        try {
//            // Étape 1 : débiter la source
//            accountClient.debit(request.sourceAccountId(), amount);
//            log.info("Débit effectué sur le compte source {}", request.sourceAccountId());
//
//            // Étape 2 : créditer la cible
//            try {
//                accountClient.credit(request.targetAccountId(), amount);
//                log.info("Crédit effectué sur le compte cible {}", request.targetAccountId());
//            } catch (FeignException e) { // couvre aussi RetryableException
//                log.error("Crédit impossible sur le compte {} : {}", request.targetAccountId(), e.getMessage());
//                compensate(request, amount);
//                throw e;
//            }
//
//            Transaction saved = repository.save(new Transaction(
//                    request.sourceAccountId(), request.targetAccountId(),
//                    request.amount(), TransactionStatus.COMPLETED));
//            log.info("Virement {} terminé avec succès", saved.getId());
//            return TransactionResponse.from(saved);
//
//        } catch (FeignException.NotFound e) {
//            log.warn("Virement échoué : compte introuvable");
//            saveFailed(request);
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte introuvable");
//        } catch (FeignException.UnprocessableEntity e) {
//            log.warn("Virement échoué : solde insuffisant sur le compte {}", request.sourceAccountId());
//            saveFailed(request);
//            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Solde insuffisant");
//        } catch (RetryableException e) {
//            log.error("Virement échoué : account-service injoignable ({})", e.getMessage());
//            saveFailed(request);
//            throw new ResponseStatusException(
//                    HttpStatus.SERVICE_UNAVAILABLE, "account-service indisponible");
//        }
//    }
//
//    public List<TransactionResponse> findAll() {
//        return repository.findAll().stream().map(TransactionResponse::from).toList();
//    }
//
//    private void compensate(TransferRequest request, AmountDto amount) {
//        try {
//            accountClient.credit(request.sourceAccountId(), amount);
//            log.warn("Crédit cible échoué : source {} remboursée", request.sourceAccountId());
//        } catch (Exception ex) {
//            log.error("COMPENSATION ÉCHOUÉE : le compte {} a été débité de {} sans crédit de la cible",
//                    request.sourceAccountId(), request.amount());
//        }
//    }
//
//    private void saveFailed(TransferRequest request) {
//        repository.save(new Transaction(
//                request.sourceAccountId(), request.targetAccountId(),
//                request.amount(), TransactionStatus.FAILED));
//    }
//}


//package org.moustapha.transactionservice.service;
//import feign.FeignException;
//import feign.RetryableException;
//import org.moustapha.transactionservice.client.AccountClient;
//import org.moustapha.transactionservice.dto.AmountDto;
//import org.moustapha.transactionservice.dto.TransactionResponse;
//import org.moustapha.transactionservice.dto.TransferRequest;
//import org.moustapha.transactionservice.model.Transaction;
//import org.moustapha.transactionservice.model.TransactionStatus;
//import org.moustapha.transactionservice.repository.TransactionRepository;
//import org.slf4j.LoggerFactory;
//import org.springframework.http.HttpStatus;
//import org.springframework.stereotype.Service;
//import org.springframework.web.server.ResponseStatusException;
//import org.slf4j.Logger;
//import java.util.List;
//
//
//@Service
//public class TransactionService {
//
//    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
//    private final TransactionRepository repository;
//    private final AccountClient accountClient;
//
//    public TransactionService(TransactionRepository repository, AccountClient accountClient) {
//        this.repository = repository;
//        this.accountClient = accountClient;
//    }
//
//    public TransactionResponse transfer(TransferRequest request) {
//        if (request.sourceAccountId().equals(request.targetAccountId())) {
//            throw new ResponseStatusException(
//                    HttpStatus.BAD_REQUEST, "Les deux comptes doivent être différents");
//        }
//
//        AmountDto amount = new AmountDto(request.amount());
//
//        try {
//            // Étape 1 : débiter la source
//            accountClient.debit(request.sourceAccountId(), amount);
//
//            // Étape 2 : créditer la cible
//            try {
//                accountClient.credit(request.targetAccountId(), amount);
//            } catch (FeignException e) { // couvre aussi RetryableException
//                // Le débit est déjà fait : on rembourse la source
//                compensate(request, amount);
//                throw e;
//            }
//
//            Transaction saved = repository.save(new Transaction(
//                    request.sourceAccountId(), request.targetAccountId(),
//                    request.amount(), TransactionStatus.COMPLETED));
//            return TransactionResponse.from(saved);
//
//        } catch (FeignException.NotFound e) {
//            saveFailed(request);
//            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Compte introuvable");
//        } catch (FeignException.UnprocessableEntity e) {
//            saveFailed(request);
//            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Solde insuffisant");
//        } catch (RetryableException e) {
//            saveFailed(request);
//            throw new ResponseStatusException(
//                    HttpStatus.SERVICE_UNAVAILABLE, "account-service indisponible");
//        }
//    }
//
//    public List<TransactionResponse> findAll() {
//        return repository.findAll().stream().map(TransactionResponse::from).toList();
//    }
//
//    private void compensate(TransferRequest request, AmountDto amount) {
//        try {
//            accountClient.credit(request.sourceAccountId(), amount);
//            log.warn("Crédit cible échoué : source {} remboursée", request.sourceAccountId());
//        } catch (Exception ex) {
//            log.error("COMPENSATION ÉCHOUÉE : le compte {} a été débité de {} sans crédit de la cible",
//                    request.sourceAccountId(), request.amount());
//        }
//    }
//
//    private void saveFailed(TransferRequest request) {
//        repository.save(new Transaction(
//                request.sourceAccountId(), request.targetAccountId(),
//                request.amount(), TransactionStatus.FAILED));
//    }
//}
