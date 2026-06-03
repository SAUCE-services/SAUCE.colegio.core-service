package ar.com.sauce.colegio.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FacturaPeriodoDetalleDto {
    private Long factura;
    private String periodo;
    private Long legajo;
    private String nombre;
    private BigDecimal facturado; // Usamos 'facturado' para diferenciarlo de 'pagado'
}