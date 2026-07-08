package ar.com.sauce.colegio.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FacturarCursoRequestDto {
    private Long cursoId;
    private Long periodoId;
    private LocalDate fechaVencimiento;
}