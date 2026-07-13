package ar.com.sauce.colegio.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FacturarAlumnoRequestDto {
    private Long alumnoId;
    private Long periodoId;
    private LocalDate fechaVencimiento;
    private BigDecimal recargo; // Opcional: se suma al total, no genera un concepto aparte
}