package ar.com.sauce.colegio.rest.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface ConceptoDetalleProjection {
    String getDescripcion();
    BigDecimal getImporte();
    LocalDate getFechaRegistro();
}