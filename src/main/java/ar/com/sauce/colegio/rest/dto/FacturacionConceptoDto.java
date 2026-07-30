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
public class FacturacionConceptoDto {
    private String nombreConcepto;
    private List<ConceptoDetalleAlumnoDto> detalles = new ArrayList<>();
    private Integer cantidadFacturado;
    private BigDecimal totalFacturado;
    private Integer cantidadCobrado;
    private BigDecimal totalCobrado;
}