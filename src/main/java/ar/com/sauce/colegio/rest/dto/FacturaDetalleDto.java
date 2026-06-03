package ar.com.sauce.colegio.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FacturaDetalleDto {
    private Long nroFactura;        // Factura
    private String estado;           // Estado
    private LocalDate fechaEstado;   // F.Estado
    private LocalDate primerVenc;    // 1er Venc
    private LocalDate fechaPago;     // F.Pago
    private BigDecimal impAdeudado;  // Imp.Adeudac
    private BigDecimal impPagado;    // Imp.Pagado
    private LocalDate fechaCanc;     // F.Canc.
    private String periodo;          // Periodo
}