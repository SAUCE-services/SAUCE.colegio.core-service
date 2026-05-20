package ar.com.sauce.colegio.rest.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DeudaCursoProjection {
    Long getIdCurso();
    String getCurso();
    Long getLegajo();
    String getDni();
    String getAlumno();
    Long getFactura();
    String getPeriodo();
    LocalDate getVencimiento();
    BigDecimal getImporte();
}