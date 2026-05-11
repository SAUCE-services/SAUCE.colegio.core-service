package ar.com.sauce.colegio.rest.dto;

import lombok.Data;

@Data
public class RecaudacionDto {
    private String establecimiento;
    private String factura;
    private String periodo;
    private Integer legajo;
    private String nombre;
    private String fecha; // fecha_pago
    private Double pagado; // importe_pagado
}
