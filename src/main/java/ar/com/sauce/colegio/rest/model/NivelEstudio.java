package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

@Data
@Entity
@Table(name = "niveles_estudio")
@AttributeOverride(name = "updated",
        column = @Column(name = "created", insertable = false, updatable = false))
public class NivelEstudio extends Auditable implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nivel_estudio")
    private Long idNivelEstudio;

    @Column(name = "descripcion")
    private String descripcion;
}
