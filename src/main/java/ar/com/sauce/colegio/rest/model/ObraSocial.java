package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

@Data
@Entity
@Table(name = "o_sociales")
@AttributeOverride(name = "updated",
        column = @Column(name = "created", insertable = false, updatable = false))
public class ObraSocial extends Auditable implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_osocial")
    private Long idObraSocial;

    @Column(name = "descripcion")
    private String descripcion;
}
