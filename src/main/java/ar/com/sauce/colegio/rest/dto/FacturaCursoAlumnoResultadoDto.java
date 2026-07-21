package ar.com.sauce.colegio.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FacturaCursoAlumnoResultadoDto {
    private Long legajo;
    private String nombreCompleto;
    private Long nroFactura;
    private BigDecimal importeTotal;
    private List<LineaDetalleDto> conceptos;
}