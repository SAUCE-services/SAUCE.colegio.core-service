package ar.com.sauce.colegio.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class NovedadCursoDto {
    private Long legajo;
    private String alumno;
    private String curso;
    private String concepto;
    private BigDecimal importe;
    private String estado;
    private LocalDate fecha;
}