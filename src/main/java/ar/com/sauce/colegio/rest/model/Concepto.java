package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

@Data
@Entity
@Table(name = "conceptos")
public class Concepto extends Auditable implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_concepto")
    private Long idConcepto;

    @Column(name = "descripcion")
    private String descripcion;

    @Column(name = "importe")
    private BigDecimal importe;

}
