package ar.com.sauce.colegio.rest.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DeudaGeneralProjection {
    Long getIdAlumno();
    String getLegajo();
    String getDni();
    String getAlumno();
    String getFactura();
    String getPeriodo();
    LocalDate getVencimiento();
    BigDecimal getImporte();
}