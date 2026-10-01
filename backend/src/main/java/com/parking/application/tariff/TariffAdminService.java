package com.parking.application.tariff;

import com.parking.application.audit.AuditService;
import static com.parking.application.audit.AuditJson.str;
import com.parking.domain.exception.NotFoundException;
import com.parking.domain.model.Tariff;
import com.parking.domain.model.TariffModality;
import com.parking.domain.model.TariffRounding;
import com.parking.domain.model.TariffTimeUnit;
import com.parking.domain.repository.TariffRepository;
import com.parking.domain.repository.VehicleTypeRepository;
import com.parking.domain.time.TimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

/** UC-008 (RF-018 a RF-022). BR-005, BR-006, BR-009, BR-025. */
@Service
public class TariffAdminService {

    private final TariffRepository tariffRepository;
    private final VehicleTypeRepository vehicleTypeRepository;
    private final AuditService auditService;
    private final TimeProvider timeProvider;

    public TariffAdminService(TariffRepository tariffRepository, VehicleTypeRepository vehicleTypeRepository,
                               AuditService auditService, TimeProvider timeProvider) {
        this.tariffRepository = tariffRepository;
        this.vehicleTypeRepository = vehicleTypeRepository;
        this.auditService = auditService;
        this.timeProvider = timeProvider;
    }

    public List<Tariff> list(String vehicleTypeId, Boolean active) {
        return tariffRepository.findAll(vehicleTypeId, active);
    }

    public Tariff getById(String id) {
        return tariffRepository.findById(id).orElseThrow(() -> new NotFoundException("Tarifa no encontrada"));
    }

    /** RF-018, RF-021 (validación de consistencia por modalidad). */
    public record TariffInput(
            String name, String description, String vehicleTypeId, TariffModality modality, BigDecimal value,
            LocalTime startTime, LocalTime endTime, Integer toleranceMinutes, Integer minTimeMinutes,
            TariffTimeUnit timeUnit, Integer fractionMinutes, TariffRounding rounding, Integer priority,
            Boolean active, LocalDate validFrom, LocalDate validTo) {
    }

    @Transactional
    public Tariff create(TariffInput input, String actorId) {
        validate(input);
        Instant now = timeProvider.now();
        Tariff tariff = new Tariff(UUID.randomUUID().toString(), input.name(), input.description(),
                input.vehicleTypeId(), input.modality(), input.value(), input.startTime(), input.endTime(),
                input.toleranceMinutes(), input.minTimeMinutes(), input.timeUnit(), input.fractionMinutes(),
                input.rounding() != null ? input.rounding() : TariffRounding.UP,
                input.priority() != null ? input.priority() : 1,
                input.active() == null || input.active(), input.validFrom() != null ? input.validFrom() : LocalDate.now(),
                input.validTo(), now, now, actorId, actorId);
        Tariff saved = tariffRepository.save(tariff);

        auditService.record(actorId, "TARIFF_CREATED", "Tariff", saved.id(), null, describe(saved));
        return saved;
    }

    @Transactional
    public Tariff update(String id, TariffInput input, String actorId) {
        Tariff existing = getById(id);
        validate(input);
        Instant now = timeProvider.now();
        Tariff updated = new Tariff(existing.id(), input.name(), input.description(), input.vehicleTypeId(),
                input.modality(), input.value(), input.startTime(), input.endTime(), input.toleranceMinutes(),
                input.minTimeMinutes(), input.timeUnit(), input.fractionMinutes(),
                input.rounding() != null ? input.rounding() : existing.rounding(),
                input.priority() != null ? input.priority() : existing.priority(),
                input.active() != null ? input.active() : existing.active(),
                input.validFrom() != null ? input.validFrom() : existing.validFrom(), input.validTo(),
                existing.createdAt(), now, existing.createdBy(), actorId);
        Tariff saved = tariffRepository.save(updated);

        auditService.record(actorId, "TARIFF_UPDATED", "Tariff", id, describe(existing), describe(saved));
        return saved;
    }

    @Transactional
    public Tariff setActive(String id, boolean active, String actorId) {
        Tariff existing = getById(id);
        Tariff updated = new Tariff(existing.id(), existing.name(), existing.description(),
                existing.vehicleTypeId(), existing.modality(), existing.value(), existing.startTime(),
                existing.endTime(), existing.toleranceMinutes(), existing.minTimeMinutes(), existing.timeUnit(),
                existing.fractionMinutes(), existing.rounding(), existing.priority(), active, existing.validFrom(),
                existing.validTo(), existing.createdAt(), timeProvider.now(), existing.createdBy(), actorId);
        Tariff saved = tariffRepository.save(updated);

        auditService.record(actorId, active ? "TARIFF_ACTIVATED" : "TARIFF_DEACTIVATED", "Tariff", id,
                "{\"active\":" + existing.active() + "}", "{\"active\":" + active + "}");
        return saved;
    }

    private void validate(TariffInput input) {
        if (input.name() == null || input.name().isBlank()) {
            throw new IllegalArgumentException("El nombre de la tarifa es obligatorio");
        }
        if (vehicleTypeRepository.findById(input.vehicleTypeId()).isEmpty()) {
            throw new IllegalArgumentException("Tipo de vehículo inválido");
        }
        if (input.value() == null || input.value().signum() < 0) {
            throw new IllegalArgumentException("El valor de la tarifa debe ser mayor o igual a 0");
        }
        if (input.modality() == null) {
            throw new IllegalArgumentException("La modalidad de la tarifa es obligatoria");
        }
        switch (input.modality()) {
            case FULL_AM, FULL_PM, FULL_DAY -> {
                if (input.startTime() == null || input.endTime() == null) {
                    throw new IllegalArgumentException(
                            "Las tarifas " + input.modality() + " requieren hora inicial y final");
                }
            }
            case BY_TIME -> {
                if (input.fractionMinutes() == null || input.fractionMinutes() <= 0) {
                    throw new IllegalArgumentException("Las tarifas por tiempo requieren una fracción mayor a 0");
                }
            }
        }
        if (input.validFrom() != null && input.validTo() != null && input.validTo().isBefore(input.validFrom())) {
            throw new IllegalArgumentException("La fecha de fin de vigencia no puede ser anterior al inicio");
        }
    }

    /**
     * Resumen para auditoria (BR-025): todos los campos que afectan el calculo, incluido el horario,
     * para que un TARIFF_UPDATED muestre que cambio (antes solo se guardaba nombre, modalidad, valor,
     * prioridad y estado, y no se podia saber que horario tenia la tarifa antes de editarla).
     */
    private static String describe(Tariff t) {
        return "{\"name\":" + str(t.name())
                + ",\"vehicleTypeId\":" + str(t.vehicleTypeId())
                + ",\"modality\":" + str(t.modality() == null ? null : t.modality().name())
                + ",\"value\":" + t.value()
                + ",\"startTime\":" + str(t.startTime() == null ? null : t.startTime().toString())
                + ",\"endTime\":" + str(t.endTime() == null ? null : t.endTime().toString())
                + ",\"toleranceMinutes\":" + t.toleranceMinutes()
                + ",\"fractionMinutes\":" + t.fractionMinutes()
                + ",\"rounding\":" + str(t.rounding() == null ? null : t.rounding().name())
                + ",\"priority\":" + t.priority()
                + ",\"active\":" + t.active()
                + ",\"validFrom\":" + str(t.validFrom() == null ? null : t.validFrom().toString())
                + ",\"validTo\":" + str(t.validTo() == null ? null : t.validTo().toString()) + "}";
    }
}
