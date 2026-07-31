package ar.com.sauce.colegio.rest.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface FacturacionConceptoProjection {
    String getConcepto();
    Long getLegajo();
    String getNombreAlumno();
    Long getNroFactura();
    BigDecimal getImporte();
    LocalDate getFechaFactura();
    LocalDate getFechaPago();
    String getPeriodo();
    Integer getPagado(); // 1 = pagada, 0 = no pagada (CASE WHEN nativo de MySQL)
}