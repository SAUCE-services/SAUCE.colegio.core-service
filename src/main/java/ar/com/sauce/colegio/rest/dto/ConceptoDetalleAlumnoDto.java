package ar.com.sauce.colegio.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConceptoDetalleAlumnoDto {
    private Long legajo;
    private String nombreAlumno;
    private Long nroFactura;
    private BigDecimal importe;
    private LocalDate fechaFactura;
    private LocalDate fechaPago;
    private String periodo;
    private boolean pagado;
}