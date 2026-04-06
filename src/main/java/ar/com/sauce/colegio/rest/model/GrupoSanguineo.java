package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

@Data
@Entity
@Table(name = "grupos_sanguineos")
@AttributeOverride(name = "updated",
        column = @Column(name = "created", insertable = false, updatable = false))
public class GrupoSanguineo extends Auditable implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_grupo_san")
    private Long idGrupoSanguineo;

    @Column(name = "descripcion")
    private String descripcion;

}
