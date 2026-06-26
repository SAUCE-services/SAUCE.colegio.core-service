package ar.com.sauce.colegio.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CursoCargaDto {
    private Long cursoId; // Será null o 0 para creación, y el ID real para modificación
    private String descripcion;
    private Long turnoId;
    private Long maestroId;
    private Long establecimientoId;
    private Long cicloId;
}