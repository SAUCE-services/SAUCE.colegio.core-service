package ar.com.sauce.colegio.rest.dto;

import lombok.Data;
import java.time.LocalDate;

@Data
public class PeriodoDto {
    private Long periodoId;      // Para la columna #ID
    private String descripcion;  // Para la columna Período
    private String nombreCiclo;  // Para la columna Ciclo
    private LocalDate mes;       // Primer Vencimiento
    private LocalDate fechaSegundo; // Segundo Vencimiento
}