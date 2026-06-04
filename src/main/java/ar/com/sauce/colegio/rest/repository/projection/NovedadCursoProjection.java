package ar.com.sauce.colegio.rest.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface NovedadCursoProjection {
    Long getLegajo();
    String getAlumno();
    String getCursoNombre();
    String getConcepto();
    BigDecimal getImporte();
    String getEstado();
    LocalDate getFechaRegistro();
}