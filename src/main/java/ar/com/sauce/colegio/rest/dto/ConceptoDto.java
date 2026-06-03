package ar.com.sauce.colegio.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ConceptoDto {
    private Long conceptoId;  // Corresponde a #ID en la imagen
    private String descripcion; // Corresponde a Concepto
    private BigDecimal importe; // Corresponde a Importe
}