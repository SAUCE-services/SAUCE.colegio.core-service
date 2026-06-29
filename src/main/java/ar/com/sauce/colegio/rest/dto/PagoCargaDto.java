package ar.com.sauce.colegio.rest.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class PagoCargaDto {
    private Long legajo;
    private Long nroFactura;
    private LocalDate fechaPago;
    private BigDecimal importePagado;
    private Long tipoPagoId; // Mapeado al combo/medio de pago (Manual, PagoFácil, etc.)
}