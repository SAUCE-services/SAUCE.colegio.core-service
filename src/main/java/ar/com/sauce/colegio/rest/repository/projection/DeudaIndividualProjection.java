package ar.com.sauce.colegio.rest.repository.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DeudaIndividualProjection {
    LocalDate getFechaEstado();   // 👈 Mapea con fechaEstado
    String getConcepto();         // 👈 Mapea con concepto
    String getEstado();           // 👈 Mapea con estado
    BigDecimal getImporte();       // 👈 Mapea con importe
    LocalDate getFechaRegistro(); // 👈 Mapea con fechaRegistro
    String getPeriodo();          // 👈 Mapea con periodo
}