package ar.com.sauce.colegio.rest.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DeudaGeneralDto {
    private Long idAlumno;
    private String legajo;
    private String dni;
    private String nombreAlumno;
    private List<FacturaPendienteDto> facturas;
    private BigDecimal totalDeudaAlumno;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class FacturaPendienteDto {
        private String nroFactura;
        private String periodo;
        private LocalDate fechaVencimiento;
        private BigDecimal importe;

    }
}