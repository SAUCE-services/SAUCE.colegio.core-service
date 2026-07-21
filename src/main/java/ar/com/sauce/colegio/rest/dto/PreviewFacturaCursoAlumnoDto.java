package ar.com.sauce.colegio.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PreviewFacturaCursoAlumnoDto {
    private Long legajo;
    private String nombreCompleto;
    private boolean facturado;              // true = ya tiene AL MENOS una Factura para este período
    private Long nroFactura;                // null si todavía no tiene ninguna
    private BigDecimal importeFactura;      // null si todavía no tiene ninguna
    private boolean tienePendientes;        // true = tiene al menos un concepto sin facturar
    private List<LineaDetalleDto> conceptos; // TODOS los conceptos del alumno en el período (facturados y pendientes)
}