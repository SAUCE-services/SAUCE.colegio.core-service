package ar.com.sauce.colegio.rest.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Factura extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_facturas")
    private Long facturaId;

    private Long nroFactura;

    private LocalDate fechaPago;

    private LocalDate fechaEstado;

    private Integer impresa;

    @Column(name = "pri_venc")
    private LocalDate primerVencimiento;

    private BigDecimal importePagado;

    private BigDecimal importeAdeudado;

    private LocalDate fechaCancelacion;

    private String pfCodigo;

    private String pfBarras;

    @Column(name = "cajamovimiento_id")
    private Long cajaMovimientoId;

    @ManyToOne // Muchos facturas están en el mismo Estado
    @JoinColumn(name = "id_estado")
    private TipoEstado tipoEstado;

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
