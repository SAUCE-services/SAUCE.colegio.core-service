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
public class ReporteFacturacionConceptoDto {
    private String filtroDescripcion; // Ej: "JUNIO - 2026" o "01/06/2026 al 30/06/2026"
    private LocalDateTime fechaGeneracion;
    private List<FacturacionConceptoDto> conceptos = new ArrayList<>();
    private BigDecimal granTotalFacturado;
    private BigDecimal granTotalCobrado;
    private Integer cantidadTotalFacturado;
    private Integer cantidadTotalCobrado;
}