package com.parking.application.configuration;

import com.parking.application.audit.AuditService;
import static com.parking.application.audit.AuditJson.num;
import static com.parking.application.audit.AuditJson.str;
import com.parking.domain.exception.NotFoundException;
import com.parking.domain.model.Configuration;
import com.parking.domain.repository.ConfigurationRepository;
import com.parking.domain.time.TimeProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** UC-013 (RF-010 a RF-015). BR-025 ("modificaciones administrativas"). */
@Service
public class ConfigurationService {

    private final ConfigurationRepository configurationRepository;
    private final AuditService auditService;
    private final TimeProvider timeProvider;

    public ConfigurationService(ConfigurationRepository configurationRepository, AuditService auditService,
                                 TimeProvider timeProvider) {
        this.configurationRepository = configurationRepository;
        this.auditService = auditService;
        this.timeProvider = timeProvider;
    }

    public Configuration get() {
        return configurationRepository.find()
                .orElseThrow(() -> new NotFoundException("Configuración no inicializada"));
    }

    public record ConfigurationInput(String parkingName, String nit, String address, String phone,
                                      Integer maxCapacity, String timezone, String currency,
                                      String receiptFormatJson) {
    }

    @Transactional
    public Configuration update(ConfigurationInput input, String actorId) {
        validateReceiptFormat(input.receiptFormatJson());
        Configuration existing = get();
        Configuration updated = new Configuration(existing.id(), input.parkingName(), input.nit(),
                input.address(), input.phone(), input.maxCapacity(),
                input.timezone() != null ? input.timezone() : existing.timezone(),
                input.currency() != null ? input.currency() : existing.currency(), input.receiptFormatJson(),
                timeProvider.now(), actorId);
        Configuration saved = configurationRepository.save(updated);

        // BR-025: valor anterior y nuevo de todos los campos (antes se registraba null/null y no se
        // podia saber que habia cambiado).
        auditService.record(actorId, "CONFIGURATION_UPDATED", "Configuration", existing.id(),
                describe(existing), describe(saved));
        return saved;
    }

    /**
     * RF-015: el formato del comprobante (campos visibles y pie de pagina) se guarda como un objeto
     * JSON; se rechaza cualquier otro valor para no romper el comprobante.
     */
    private static void validateReceiptFormat(String json) {
        if (json == null || json.isBlank()) return;
        if (json.length() > 2000) {
            throw new IllegalArgumentException("El formato del comprobante es demasiado largo");
        }
        try {
            if (!tools.jackson.databind.json.JsonMapper.builder().build().readTree(json).isObject()) {
                throw new IllegalArgumentException("El formato del comprobante debe ser un objeto JSON");
            }
        } catch (tools.jackson.core.JacksonException e) {
            throw new IllegalArgumentException("El formato del comprobante no es un JSON válido");
        }
    }

    private static String describe(Configuration c) {
        return "{\"parkingName\":" + str(c.parkingName())
                + ",\"nit\":" + str(c.nit())
                + ",\"address\":" + str(c.address())
                + ",\"phone\":" + str(c.phone())
                + ",\"maxCapacity\":" + num(c.maxCapacity())
                + ",\"timezone\":" + str(c.timezone())
                + ",\"currency\":" + str(c.currency())
                + ",\"receiptFormatJson\":" + str(c.receiptFormatJson()) + "}";
    }
}
