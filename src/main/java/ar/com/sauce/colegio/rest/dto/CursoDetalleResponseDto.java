package ar.com.sauce.colegio.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CursoDetalleResponseDto {
    // Datos de la cabecera (para los campos de arriba en tu imagen)
    private String nombreMaestro;
    private String nombreEstablecimiento;
    private String nombreTurno;
    private String nombreCiclo;

    // Lista de alumnos (para la grilla)
    private List<AlumnoDto> alumnos;
}