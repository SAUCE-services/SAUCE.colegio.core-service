package ar.com.sauce.colegio.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LineaDetalleDto {
    private LocalDate fechaEstado;   // F.Estado
    private String concepto;         // Concepto
    private String estado;           // Estado
    private BigDecimal importe;      // Importe
    private LocalDate fechaRegistro; // F.Registro
    private String periodo;          // Periodo
}