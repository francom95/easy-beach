package com.easybeach.promotions.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.easybeach.promotions.domain.EstadoPromocion;
import com.easybeach.promotions.domain.Promocion;
import com.easybeach.promotions.domain.TipoPromocion;
import com.easybeach.shared.time.ZonaNegocio;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import org.junit.jupiter.api.Test;

/**
 * Etapa 14 criterio de aceptación: "bordes de vigencia horaria y de fechas,
 * con la TZ correcta". Unitario: fija el instante evaluado en vez de
 * depender del reloj real.
 */
class VigenciaPromocionCheckerTest {

    private final VigenciaPromocionChecker checker = new VigenciaPromocionChecker();

    private Promocion promocionBase(TipoPromocion tipo) {
        Promocion p = new Promocion();
        p.setEstado(EstadoPromocion.ACTIVA);
        p.setTipo(tipo);
        p.setValor(BigDecimal.TEN);
        return p;
    }

    private ZonedDateTime enBuenosAires(String isoLocal) {
        return ZonedDateTime.of(java.time.LocalDateTime.parse(isoLocal), ZonaNegocio.ZONE_ID);
    }

    @Test
    void promocionInactivaNuncaEstaVigente() {
        Promocion p = promocionBase(TipoPromocion.DESCUENTO_PORCENTUAL);
        p.setEstado(EstadoPromocion.INACTIVA);
        assertThat(checker.estaVigenteEn(p, enBuenosAires("2026-01-15T12:00:00"))).isFalse();
    }

    @Test
    void sinVigenciaDeFechasEsSiempreVigente() {
        Promocion p = promocionBase(TipoPromocion.DESCUENTO_PORCENTUAL);
        assertThat(checker.estaVigenteEn(p, enBuenosAires("2026-01-15T12:00:00"))).isTrue();
    }

    @Test
    void bordeDeVigenciaDesdeEsInclusivo() {
        Promocion p = promocionBase(TipoPromocion.DESCUENTO_PORCENTUAL);
        p.setVigenciaDesde(LocalDate.parse("2026-01-15"));
        assertThat(checker.estaVigenteEn(p, enBuenosAires("2026-01-15T00:00:00"))).isTrue();
        assertThat(checker.estaVigenteEn(p, enBuenosAires("2026-01-14T23:59:59"))).isFalse();
    }

    @Test
    void bordeDeVigenciaHastaEsInclusivo() {
        Promocion p = promocionBase(TipoPromocion.DESCUENTO_PORCENTUAL);
        p.setVigenciaHasta(LocalDate.parse("2026-01-15"));
        assertThat(checker.estaVigenteEn(p, enBuenosAires("2026-01-15T23:59:59"))).isTrue();
        assertThat(checker.estaVigenteEn(p, enBuenosAires("2026-01-16T00:00:00"))).isFalse();
    }

    @Test
    void happyHourDentroDeLaFranjaEsVigente() {
        Promocion p = promocionBase(TipoPromocion.HAPPY_HOUR);
        p.setFranjaHoraDesde(LocalTime.of(18, 0));
        p.setFranjaHoraHasta(LocalTime.of(20, 0));
        assertThat(checker.estaVigenteEn(p, enBuenosAires("2026-01-15T19:00:00"))).isTrue();
    }

    @Test
    void happyHourEnElBordeDeInicioEsVigenteYEnElBordeDeFinNo() {
        Promocion p = promocionBase(TipoPromocion.HAPPY_HOUR);
        p.setFranjaHoraDesde(LocalTime.of(18, 0));
        p.setFranjaHoraHasta(LocalTime.of(20, 0));
        assertThat(checker.estaVigenteEn(p, enBuenosAires("2026-01-15T18:00:00"))).isTrue();
        assertThat(checker.estaVigenteEn(p, enBuenosAires("2026-01-15T20:00:00"))).isFalse();
    }

    @Test
    void happyHourFueraDeLaFranjaNoEsVigente() {
        Promocion p = promocionBase(TipoPromocion.HAPPY_HOUR);
        p.setFranjaHoraDesde(LocalTime.of(18, 0));
        p.setFranjaHoraHasta(LocalTime.of(20, 0));
        assertThat(checker.estaVigenteEn(p, enBuenosAires("2026-01-15T17:59:59"))).isFalse();
        assertThat(checker.estaVigenteEn(p, enBuenosAires("2026-01-15T20:00:01"))).isFalse();
    }

    @Test
    void happyHourQueCruzaMedianocheFunciona() {
        Promocion p = promocionBase(TipoPromocion.HAPPY_HOUR);
        p.setFranjaHoraDesde(LocalTime.of(22, 0));
        p.setFranjaHoraHasta(LocalTime.of(2, 0));
        assertThat(checker.estaVigenteEn(p, enBuenosAires("2026-01-15T23:30:00"))).isTrue();
        assertThat(checker.estaVigenteEn(p, enBuenosAires("2026-01-16T01:30:00"))).isTrue();
        assertThat(checker.estaVigenteEn(p, enBuenosAires("2026-01-16T12:00:00"))).isFalse();
    }

    @Test
    void happyHourConDiaDeLaSemanaRestringido() {
        Promocion p = promocionBase(TipoPromocion.HAPPY_HOUR);
        p.setDiasSemana("VIE,SAB");
        // 2026-01-15 es jueves.
        assertThat(checker.estaVigenteEn(p, enBuenosAires("2026-01-15T12:00:00"))).isFalse();
        // 2026-01-16 es viernes.
        assertThat(checker.estaVigenteEn(p, enBuenosAires("2026-01-16T12:00:00"))).isTrue();
    }

    @Test
    void diaDeLaSemanaSoloAplicaAHappyHour() {
        // Un descuento % con franja/día seteados (aunque no sea lo típico) no
        // debe filtrarse por día - esos campos son "Happy hour" por spec.
        Promocion p = promocionBase(TipoPromocion.DESCUENTO_PORCENTUAL);
        p.setDiasSemana("VIE");
        assertThat(checker.estaVigenteEn(p, enBuenosAires("2026-01-15T12:00:00"))).isTrue();
    }
}
