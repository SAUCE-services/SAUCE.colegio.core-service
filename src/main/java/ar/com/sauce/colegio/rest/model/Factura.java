package ar.com.sauce.colegio.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "factura")
public class Factura extends Auditable implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_facturas")
    private Long idFacturas;

    @Column(name = "nro_factura")
    private Long nroFactura;

    @Column(name = "fecha_pago")
    private LocalDate fechaPago;

    @Column(name = "fecha_estado")
    private LocalDate fechaEstado;

    @Column(name = "impresa")
    private Integer impresa;

    @Column(name = "pri_venc")
    private LocalDate primerVencimiento;

    @Column(name = "importe_pagado")
    private BigDecimal importePagado;

    @Column(name = "importe_adeudado")
    private BigDecimal importeAdeudado;

    @Column(name = "fecha_cancelacion")
    private LocalDate fechaCancelacion;

    @Column(name = "pf_codigo")
    private String pfCodigo;

    @Column(name = "pf_barras")
    private String pfBarras;

    @Column(name = "cajamovimiento_id")
    private Long cajaMovimientoId;

    @ManyToOne // Muchos facturas están en el mismo Estado
    @JoinColumn(name = "id_estado")
    private TipoEstado estado;

    @ManyToOne // Muchos facturas tienen el mismo TipoPago
    @JoinColumn(name = "tipo_id")
    private TipoPago tipoPago;

    @ManyToOne // Muchos facturas pertenecen a un mismo Periodo
    @JoinColumn(name = "id_periodo")
    private Periodo periodo;

    @JsonIgnoreProperties({"facturaInteres"})
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_factura_interes")
    private Factura facturaInteres;
}
