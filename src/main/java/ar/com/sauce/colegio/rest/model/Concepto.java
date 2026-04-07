package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "conceptos")
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Concepto extends AuditableSimple implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_concepto")
    private Long ConceptoId;

    private String descripcion;

    private BigDecimal importe;

}
