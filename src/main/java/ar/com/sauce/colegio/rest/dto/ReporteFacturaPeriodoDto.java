package ar.com.sauce.colegio.rest.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ReporteFacturaPeriodoDto {
    private String descripcionPeriodo; // Ej: "ABRIL - 2026"
    private LocalDateTime fechaGeneracion;
    private List<FacturaPeriodoEstablecimientoDto> establecimientos = new ArrayList<>();
    private BigDecimal granTotal;
    private Integer cantidadTotalFacturas;
}