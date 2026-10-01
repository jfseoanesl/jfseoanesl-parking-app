package com.parking.domain.repository;

import com.parking.domain.model.CashMovement;

public interface CashMovementRepository {

    CashMovement save(CashMovement movement);
}
