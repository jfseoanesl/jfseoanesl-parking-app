package com.parking.domain.repository;

import com.parking.domain.model.CashConcept;

import java.util.List;
import java.util.Optional;

/** RF-079: catálogo de conceptos de otros ingresos y gastos. */
public interface CashConceptRepository {

    CashConcept save(CashConcept concept);

    Optional<CashConcept> findById(String id);

    List<CashConcept> findAll();
}
