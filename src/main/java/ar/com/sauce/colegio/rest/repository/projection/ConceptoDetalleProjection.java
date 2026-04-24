package ar.com.sauce.colegio.rest.repository.projection;

import java.math.BigDecimal;

public interface ConceptoDetalleProjection {
    String getDescripcion();
    BigDecimal getImporte();
}