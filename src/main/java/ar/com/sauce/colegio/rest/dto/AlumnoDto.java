package ar.com.sauce.colegio.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AlumnoDto {
    private Long alumnoId;      // Para la columna Legajo
    private String nombreCompleto; // Concatenación de "Apellido, Nombre"
}