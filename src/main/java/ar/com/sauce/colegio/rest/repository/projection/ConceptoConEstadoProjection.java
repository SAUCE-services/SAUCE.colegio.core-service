package ar.com.sauce.colegio.rest.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ConceptoConEstadoProjection {
    String getDescripcion();
    BigDecimal getImporte();
    LocalDate getFechaRegistro();
    LocalDate getFechaEstado();   // Fecha de la Factura vinculada; null si todavía está pendiente
    Long getFacturado();          // 1 = ya tiene id_facturas asignado (distinto de null/0), 0 = pendiente
}