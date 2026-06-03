package ar.com.sauce.colegio.rest.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FacturaPeriodoEstablecimientoDto {
    private String nombre;
    private List<RecaudacionDetalleDto> items = new ArrayList<>();
    private Integer cantidadFacturas; // "Cantidad de Pagos" en la foto, lo llamaremos facturas
    private BigDecimal totalEstablecimiento;
}