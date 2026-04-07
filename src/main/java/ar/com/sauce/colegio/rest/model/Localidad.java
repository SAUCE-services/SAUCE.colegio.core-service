package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "localidades")
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Localidad extends AuditableSimple implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_localidad")
    private Long localidadId;

    private String descripcion;

    // RELACIONES
    @ManyToOne
    @JoinColumn(name = "id_departamento")
    private Departamento departamento;
}
