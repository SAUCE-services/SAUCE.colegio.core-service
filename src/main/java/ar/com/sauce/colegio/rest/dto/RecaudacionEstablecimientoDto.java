package ar.com.sauce.colegio.rest.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
public class RecaudacionEstablecimientoDto {
    private String nombre;
    private List<RecaudacionMedioDto> medios = new ArrayList<>();
    private BigDecimal totalEstablecimiento;
}