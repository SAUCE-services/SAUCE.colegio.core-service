package ar.com.sauce.colegio.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RecaudacionDetalleDto {
    private Long factura;
    private String periodo;
    private Long legajo;
    private String nombre;
    private LocalDate fecha; // Agregar este campo
    private BigDecimal pagado;
}