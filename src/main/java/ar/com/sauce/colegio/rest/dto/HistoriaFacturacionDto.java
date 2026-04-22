package ar.com.sauce.colegio.rest.dto;

import lombok.Data;

import java.util.List;

@Data
public class HistoriaFacturacionDto {
    private Long legajo; // id_alumno
    private String nombreCompleto; // Apellido, Nombre
    private List<FacturaDetalleDto> facturas;
}
