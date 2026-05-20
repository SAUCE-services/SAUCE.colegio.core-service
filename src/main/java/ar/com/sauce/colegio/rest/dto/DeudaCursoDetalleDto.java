package ar.com.sauce.colegio.rest.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeudaCursoDetalleDto {
    private Long legajo;
    private String dni;
    private String alumno;
    private Long factura;
    private String periodo;
    private LocalDate vencimiento;
    private BigDecimal importe;
}