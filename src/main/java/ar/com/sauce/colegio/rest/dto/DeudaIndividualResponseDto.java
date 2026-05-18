package ar.com.sauce.colegio.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeudaIndividualResponseDto implements Serializable {
    private List<LineaDetalleDto> detalles;
    private BigDecimal totalDeuda;

}