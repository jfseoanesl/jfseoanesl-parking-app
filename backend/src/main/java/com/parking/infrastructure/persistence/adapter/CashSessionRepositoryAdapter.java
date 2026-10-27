package com.parking.infrastructure.persistence.adapter;

import com.parking.domain.model.CashSession;
import com.parking.domain.model.CashSessionStatus;
import com.parking.domain.repository.CashSessionRepository;
import com.parking.infrastructure.persistence.entity.CashSessionEntity;
import com.parking.infrastructure.persistence.jpa.SpringDataCashSessionRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
public class CashSessionRepositoryAdapter implements CashSessionRepository {

    private final SpringDataCashSessionRepository springDataRepository;

    public CashSessionRepositoryAdapter(SpringDataCashSessionRepository springDataRepository) {
        this.springDataRepository = springDataRepository;
    }

    @Override
    public CashSession save(CashSession cashSession) {
        return toDomain(springDataRepository.save(toEntity(cashSession)));
    }

    @Override
    public Optional<CashSession> findById(String id) {
        return springDataRepository.findById(id).map(CashSessionRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<CashSession> findOpenByEmployeeId(String employeeId) {
        return springDataRepository.findByEmployeeIdAndStatus(employeeId, CashSessionStatus.OPEN.name())
                .map(CashSessionRepositoryAdapter::toDomain);
    }

    @Override
    public List<CashSession> findClosedByClosedAtBetween(Instant from, Instant to) {
        return springDataRepository
                .findByStatusAndClosedAtBetween(CashSessionStatus.CLOSED.name(), from, to).stream()
                .map(CashSessionRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<CashSession> findOpen() {
        return springDataRepository.findByStatus(CashSessionStatus.OPEN.name()).stream()
                .map(CashSessionRepositoryAdapter::toDomain).toList();
    }

    @Override
    public long countOpen() {
        return springDataRepository.countByStatus(CashSessionStatus.OPEN.name());
    }

    static CashSession toDomain(CashSessionEntity e) {
        return new CashSession(e.getId(), e.getEmployeeId(), CashSessionStatus.valueOf(e.getStatus()),
                e.getOpenedAt(), e.getInitialBalance(), e.getClosedAt(), e.getCashCollected(),
                e.getElectronicCollected(), e.getOtherCollected(), e.getTotalCollected(), e.getVehiclesServed(),
                e.getExpectedCash(), e.getReportedCash(), e.getDifference(), e.getOtherIncomeCollected(),
                e.getExpensesTotal(), e.getCreatedAt(), e.getUpdatedAt());
    }

    static CashSessionEntity toEntity(CashSession s) {
        return new CashSessionEntity(s.id(), s.employeeId(), s.status().name(), s.openedAt(), s.initialBalance(),
                s.closedAt(), s.cashCollected(), s.electronicCollected(), s.otherCollected(), s.totalCollected(),
                s.vehiclesServed(), s.expectedCash(), s.reportedCash(), s.difference(),
                s.otherIncomeCollected() != null ? s.otherIncomeCollected() : BigDecimal.ZERO,
                s.expensesTotal() != null ? s.expensesTotal() : BigDecimal.ZERO, s.createdAt(), s.updatedAt());
    }
}
