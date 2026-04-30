package ar.com.sauce.colegio.rest.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;
import java.util.ArrayList;

@Data
public class RecaudacionMedioDto {
    private String nombre; // Ej: "Manual" o "PagoFácil"
    private List<RecaudacionDetalleDto> items = new ArrayList<>();
    private int cantidadPagos; // Se ve como "Cantidad de Pagos: X" en la imagen
    private BigDecimal subtotal;
}