package com.parking.domain.model;

import com.parking.domain.exception.BusinessRuleViolationException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** BR-036: formato de placa de Colombia por tipo de vehículo. */
class PlateFormatTest {

    @Test
    void detectaElTipoPorElFormato() {
        assertThat(PlateFormat.detect("ABC123")).contains(PlateFormat.CAR);
        assertThat(PlateFormat.detect("ABC12D")).contains(PlateFormat.MOTORCYCLE);
        assertThat(PlateFormat.detect("ABC12")).contains(PlateFormat.MOTORCYCLE);
        assertThat(PlateFormat.detect("AB1234")).isEqualTo(Optional.empty());
        assertThat(PlateFormat.detect("ABC1234")).isEmpty();
    }

    @Test
    void aceptaPlacasDelFormatoDeSuTipo() {
        assertThatCode(() -> PlateFormat.validate("ABC123", "CAR")).doesNotThrowAnyException();
        assertThatCode(() -> PlateFormat.validate("ABC12D", "MOTORCYCLE")).doesNotThrowAnyException();
        assertThatCode(() -> PlateFormat.validate("ABC12", "MOTORCYCLE")).doesNotThrowAnyException();
    }

    @Test
    void rechazaPlacaDeOtroTipoIndicandoCualCorresponde() {
        assertThatThrownBy(() -> PlateFormat.validate("ABC12D", "CAR"))
                .isInstanceOf(BusinessRuleViolationException.class)
                .hasMessageContaining("formato de motocicleta, no de automóvil")
                .extracting("code").isEqualTo("PLATE_FORMAT_MISMATCH");
        assertThatThrownBy(() -> PlateFormat.validate("ABC123", "MOTORCYCLE"))
                .hasMessageContaining("formato de automóvil, no de motocicleta");
    }

    @Test
    void rechazaFormatoNoReconocidoConElEjemploEsperado() {
        assertThatThrownBy(() -> PlateFormat.validate("AB1234", "CAR")).hasMessageContaining("(ABC123)");
        assertThatThrownBy(() -> PlateFormat.validate("E2E1234", "MOTORCYCLE")).hasMessageContaining("ABC12D o ABC12");
    }

    @Test
    void otrosTiposSoloUsanLaValidacionLaxa() {
        assertThatCode(() -> PlateFormat.validate("BICI01", "BICYCLE")).doesNotThrowAnyException();
    }
}
