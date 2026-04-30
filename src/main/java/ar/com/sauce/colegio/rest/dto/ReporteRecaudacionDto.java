package ar.com.sauce.colegio.rest.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.ArrayList;

@Data
public class ReporteRecaudacionDto {
    private LocalDate fechaReporte;
    private List<RecaudacionEstablecimientoDto> establecimientos = new ArrayList<>();
    private BigDecimal granTotal;
}