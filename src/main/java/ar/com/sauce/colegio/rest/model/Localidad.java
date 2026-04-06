package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

@Data
@Entity
@Table(name = "localidades")
@AttributeOverride(name = "updated",
        column = @Column(name = "created", insertable = false, updatable = false))
public class Localidad extends Auditable implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_localidad")
    private Long idLocalidad;

    @Column(name = "descripcion")
    private String descripcion;

    // RELACIONES
    @ManyToOne
    @JoinColumn(name = "id_departamento")
    private Departamento departamento;
}
