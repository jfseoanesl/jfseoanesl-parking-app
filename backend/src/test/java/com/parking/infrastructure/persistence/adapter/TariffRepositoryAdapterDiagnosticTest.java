package com.parking.infrastructure.persistence.adapter;

import com.parking.domain.model.Tariff;
import com.parking.infrastructure.persistence.entity.TariffEntity;
import com.parking.infrastructure.persistence.jpa.SpringDataTariffRepository;
import com.parking.domain.repository.TariffRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regresion: el dialecto community de Hibernate para SQLite (7.4.5.Final) no logra releer
 * columnas DATE/TIME con el driver xerial sqlite-jdbc (getTimestamp()/getDate() exigen el
 * formato estricto "yyyy-MM-dd HH:mm:ss.SSS" incluso para valores solo-fecha/hora, y lanzan
 * ParseException). Por eso TariffEntity mapea start_time/end_time/valid_from/valid_to como
 * String (ver TariffEntity, TariffRepositoryAdapter, decisions/ADR-005). Esta prueba guarda
 * contra reintroducir ese bug si alguien vuelve a mapear esos campos como LocalDate/LocalTime.
 */
@SpringBootTest
@ActiveProfiles("test")
class TariffRepositoryAdapterDiagnosticTest {

    @Autowired
    private TariffRepository tariffRepository;

    @Autowired
    private SpringDataTariffRepository springDataTariffRepository;

    private static TariffEntity baseEntity(String id) {
        TariffEntity entity = new TariffEntity();
        entity.setId(id);
        entity.setName("Diagnostico " + id);
        entity.setVehicleTypeId("00000000-0000-0000-0000-000000000101");
        entity.setModality("BY_TIME");
        entity.setValue(BigDecimal.valueOf(1000));
        entity.setToleranceMinutes(10);
        entity.setTimeUnit("MINUTES");
        entity.setFractionMinutes(60);
        entity.setRounding("UP");
        entity.setPriority(2);
        entity.setActive(true);
        entity.setValidFrom("2026-01-01");
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return entity;
    }

    @Test
    void releeUnaTarifaConHorariosNulos() {
        TariffEntity entity = baseEntity("diag-null");
        entity.setStartTime(null);
        entity.setEndTime(null);
        springDataTariffRepository.save(entity);

        List<Tariff> result = tariffRepository.findActiveForVehicleType(
                "00000000-0000-0000-0000-000000000101", LocalDate.of(2026, 9, 22));
        assertTrue(result.stream().anyMatch(t -> "diag-null".equals(t.id())));
    }

    @Test
    void releeUnaTarifaConHorariosDefinidos() {
        TariffEntity entity = baseEntity("diag-horario");
        entity.setStartTime("06:00:00");
        entity.setEndTime("22:00:00");
        springDataTariffRepository.save(entity);

        List<Tariff> result = tariffRepository.findActiveForVehicleType(
                "00000000-0000-0000-0000-000000000101", LocalDate.of(2026, 9, 22));
        Tariff saved = result.stream().filter(t -> "diag-horario".equals(t.id())).findFirst().orElseThrow();
        assertTrue(saved.startTime().equals(java.time.LocalTime.of(6, 0)));
        assertTrue(saved.endTime().equals(java.time.LocalTime.of(22, 0)));
    }
}
