package ar.com.sauce.colegio.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor

public class RecaudacionDetalleDto {
    private Long factura;
    private String periodo;
    private Long legajo;
    private String nombre;
    private BigDecimal pagado;
}