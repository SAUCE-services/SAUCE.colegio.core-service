package ar.com.sauce.colegio.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CicloLectivoDto {
    private Long cicloId;
    private String nombre;
    private LocalDate desde;
    private LocalDate hasta;
}