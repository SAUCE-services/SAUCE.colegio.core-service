package ar.com.sauce.colegio.rest.dto;

import java.math.BigDecimal;
import lombok.Data;

@Data
public class NovedadCargaDto {
    private Long alumnoId;
    private String periodoNombre; // 👈 Cambiado a String (ej: "MAYO - 2026")
    private Long conceptoId;
    private BigDecimal importe;
}