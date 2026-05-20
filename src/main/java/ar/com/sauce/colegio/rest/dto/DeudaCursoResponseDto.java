package ar.com.sauce.colegio.rest.dto;

import java.math.BigDecimal;
import java.util.List;
import lombok.Data;

@Data
public class DeudaCursoResponseDto {
    private String cursoNombre;
    private String nombreMaestro;
    private String nombreTurno;
    private String nombreCiclo;
    private String nombreEstablecimiento;
    private List<DeudaCursoDetalleDto> detalles;
    private BigDecimal totalDeudaCurso;
}