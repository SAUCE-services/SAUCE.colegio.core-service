package ar.com.sauce.colegio.rest.dto;

import java.util.List;
import lombok.Data;

@Data
public class NovedadesAlumnoResponseDto {
    private Long legajo;
    private String nombreCompleto;
    private List<LineaDetalleDto> detallesGrilla; // Reutiliza tu DTO existente para la grilla
}