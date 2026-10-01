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

    public DashboardService(ParkingSessionRepository parkingSessionRepository, PaymentRepository paymentRepository,
                             CashSessionRepository cashSessionRepository,
                             ConfigurationRepository configurationRepository, TimeProvider timeProvider) {
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
                .map(p -> p.chargedAmount()).reduce(BigDecimal.ZERO, BigDecimal::add);

        long openCashSessions = cashSessionRepository.countOpen();

        var recentMovements = parkingSessionRepository.findRecent().stream()
                .map(DashboardService::toMovement)
                .toList();

        return new DashboardData(occupied, maxCapacity, entriesToday, exitsToday, collectedToday, openCashSessions,
                recentMovements);
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
