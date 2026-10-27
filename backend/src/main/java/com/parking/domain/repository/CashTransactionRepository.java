package com.parking.domain.repository;

import com.parking.domain.model.CashTransaction;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/** RF-080 a RF-084: otros ingresos y gastos de caja. */
public interface CashTransactionRepository {

    CashTransaction save(CashTransaction transaction);

    Optional<CashTransaction> findById(String id);

    /** BR-033: suman (o restan) en el arqueo de la caja donde se registraron. */
    List<CashTransaction> findByCashSessionId(String cashSessionId);

    /** RF-083/RF-084: reportes y Dashboard por rango de fechas. */
    List<CashTransaction> findByCreatedAtBetween(Instant from, Instant to);
}
