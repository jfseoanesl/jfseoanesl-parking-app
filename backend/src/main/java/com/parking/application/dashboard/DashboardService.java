package com.parking.application.dashboard;

import com.parking.domain.model.Configuration;
import com.parking.domain.model.ParkingSession;
import com.parking.domain.repository.CashSessionRepository;
import com.parking.domain.repository.ConfigurationRepository;
import com.parking.domain.repository.ParkingSessionRepository;
import com.parking.domain.repository.PaymentRepository;
import com.parking.domain.time.TimeProvider;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

/** UC — RF-064 a RF-065: pantalla principal del administrador. */
@Service
public class DashboardService {

    private final ParkingSessionRepository parkingSessionRepository;
    private final PaymentRepository paymentRepository;
    private final CashSessionRepository cashSessionRepository;
    private final ConfigurationRepository configurationRepository;
    private final TimeProvider timeProvider;
    private final com.parking.domain.repository.SubscriptionPaymentRepository subscriptionPaymentRepository;
    private final com.parking.domain.repository.CashTransactionRepository cashTransactionRepository;

    public DashboardService(ParkingSessionRepository parkingSessionRepository, PaymentRepository paymentRepository,
                             CashSessionRepository cashSessionRepository,
                             ConfigurationRepository configurationRepository, TimeProvider timeProvider,
                             com.parking.domain.repository.SubscriptionPaymentRepository subscriptionPaymentRepository,
                             com.parking.domain.repository.CashTransactionRepository cashTransactionRepository) {
        this.cashTransactionRepository = cashTransactionRepository;
        this.subscriptionPaymentRepository = subscriptionPaymentRepository;
        this.parkingSessionRepository = parkingSessionRepository;
        this.paymentRepository = paymentRepository;
        this.cashSessionRepository = cashSessionRepository;
        this.configurationRepository = configurationRepository;
        this.timeProvider = timeProvider;
    }

    public DashboardData generate() {
        ZoneId zone = timeProvider.zone();
        LocalDate today = timeProvider.nowInZone().toLocalDate();
        Instant startOfDay = today.atStartOfDay(zone).toInstant();
        Instant startOfTomorrow = today.plusDays(1).atStartOfDay(zone).toInstant();

        long occupied = parkingSessionRepository.findAllActive(null).size();
        Integer maxCapacity = configurationRepository.find().map(Configuration::maxCapacity).orElse(null);

        long entriesToday = parkingSessionRepository.findByEntryDateTimeBetween(startOfDay, startOfTomorrow).size();
        long exitsToday = parkingSessionRepository
                .findFinalizedByExitDateTimeBetween(startOfDay, startOfTomorrow).size();
        BigDecimal collectedToday = paymentRepository.findByPaidAtBetween(startOfDay, startOfTomorrow).stream()
                .map(p -> p.chargedAmount()).reduce(BigDecimal.ZERO, BigDecimal::add)
                // BR-031: las mensualidades cobradas hoy tambien son recaudo del dia (igual que en caja y reportes).
                .add(subscriptionPaymentRepository.findByPaidAtBetween(startOfDay, startOfTomorrow).stream()
                        .map(com.parking.domain.model.SubscriptionPayment::amount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add));
        // RF-084 / BR-035: otros ingresos y gastos activos de hoy; los ingresos tambien son recaudo.
        var transactionsToday = cashTransactionRepository.findByCreatedAtBetween(startOfDay, startOfTomorrow).stream()
                .filter(com.parking.domain.model.CashTransaction::isActive).toList();
        BigDecimal otherIncomeToday = sum(transactionsToday, com.parking.domain.model.CashTransactionType.INCOME);
        BigDecimal expensesToday = sum(transactionsToday, com.parking.domain.model.CashTransactionType.EXPENSE);
        collectedToday = collectedToday.add(otherIncomeToday);

        long openCashSessions = cashSessionRepository.countOpen();

        var recentMovements = parkingSessionRepository.findRecent().stream()
                .map(DashboardService::toMovement)
                .toList();

        return new DashboardData(occupied, maxCapacity, entriesToday, exitsToday, collectedToday, otherIncomeToday,
                expensesToday, openCashSessions, recentMovements);
    }

    private static BigDecimal sum(java.util.List<com.parking.domain.model.CashTransaction> transactions,
                                  com.parking.domain.model.CashTransactionType type) {
        return transactions.stream().filter(t -> t.type() == type)
                .map(com.parking.domain.model.CashTransaction::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static DashboardData.RecentMovement toMovement(ParkingSession s) {
        String description = switch (s.status()) {
            case EN_PARQUEADERO -> "Ingresó";
            case FINALIZADO -> "Salió";
            case ANULADO -> "Anulado";
        };
        return new DashboardData.RecentMovement(s.plate(), s.status().name(), description);
    }
}
