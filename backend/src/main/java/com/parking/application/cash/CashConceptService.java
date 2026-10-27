package com.parking.application.cash;

import com.parking.application.audit.AuditService;
import com.parking.domain.exception.BusinessRuleViolationException;
import com.parking.domain.exception.NotFoundException;
import com.parking.domain.model.CashConcept;
import com.parking.domain.model.CashTransactionType;
import com.parking.domain.repository.CashConceptRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import static com.parking.application.audit.AuditJson.str;

/** RF-079: catálogo de conceptos de otros ingresos y gastos (lo edita solo el administrador). */
@Service
public class CashConceptService {

    private final CashConceptRepository conceptRepository;
    private final AuditService auditService;

    public CashConceptService(CashConceptRepository conceptRepository, AuditService auditService) {
        this.conceptRepository = conceptRepository;
        this.auditService = auditService;
    }

    /** Ordenados por tipo y nombre; type y active son filtros opcionales. */
    public List<CashConcept> list(CashTransactionType type, Boolean active) {
        return conceptRepository.findAll().stream()
                .filter(c -> type == null || c.type() == type)
                .filter(c -> active == null || c.active() == active)
                .sorted(Comparator.comparing(CashConcept::type).thenComparing(c -> c.name().toLowerCase()))
                .toList();
    }

    @Transactional
    public CashConcept create(CashTransactionType type, String name, String actorId) {
        if (type == null) {
            throw new IllegalArgumentException("El tipo del concepto es obligatorio");
        }
        String clean = clean(name);
        ensureUnique(type, clean, null);
        CashConcept saved = conceptRepository.save(new CashConcept(UUID.randomUUID().toString(), type, clean, true));
        auditService.record(actorId, "CASH_CONCEPT_CREATED", "CashConcept", saved.id(), null, describe(saved));
        return saved;
    }

    @Transactional
    public CashConcept rename(String id, String name, String actorId) {
        CashConcept existing = find(id);
        String clean = clean(name);
        ensureUnique(existing.type(), clean, id);
        CashConcept saved = conceptRepository.save(new CashConcept(id, existing.type(), clean, existing.active()));
        auditService.record(actorId, "CASH_CONCEPT_UPDATED", "CashConcept", id, describe(existing), describe(saved));
        return saved;
    }

    @Transactional
    public CashConcept setActive(String id, boolean active, String actorId) {
        CashConcept existing = find(id);
        CashConcept saved = conceptRepository.save(new CashConcept(id, existing.type(), existing.name(), active));
        auditService.record(actorId, active ? "CASH_CONCEPT_ACTIVATED" : "CASH_CONCEPT_DEACTIVATED", "CashConcept",
                id, describe(existing), describe(saved));
        return saved;
    }

    CashConcept find(String id) {
        return conceptRepository.findById(id).orElseThrow(() -> new NotFoundException("Concepto no encontrado"));
    }

    private void ensureUnique(CashTransactionType type, String name, String exceptId) {
        boolean duplicated = conceptRepository.findAll().stream()
                .anyMatch(c -> c.type() == type && c.name().equalsIgnoreCase(name) && !c.id().equals(exceptId));
        if (duplicated) {
            throw new BusinessRuleViolationException("CASH_CONCEPT_DUPLICATED", "Ya existe un concepto con ese nombre");
        }
    }

    private static String clean(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("El nombre del concepto es obligatorio");
        }
        String clean = name.trim();
        if (clean.length() > 100) {
            throw new IllegalArgumentException("El nombre del concepto admite máximo 100 caracteres");
        }
        return clean;
    }

    private static String describe(CashConcept c) {
        return "{\"type\":" + str(c.type().name()) + ",\"name\":" + str(c.name()) + ",\"active\":" + c.active() + "}";
    }
}
