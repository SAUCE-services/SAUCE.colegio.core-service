package ar.com.sauce.colegio.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CursoDto {
    private Long cursoId;
    private String descripcion;
    private String nombreEstablecimiento;
    private String nombreMaestro; // "Morales, Anabela"
    private String nombreTurno;   // "Mañana"
}