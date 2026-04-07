package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "tipopago")
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class TipoPago extends AuditableSimple implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tipo_id")
    private Long tipoId;

    private String nombre;

    private Long autoId;

    private String uid;

}
