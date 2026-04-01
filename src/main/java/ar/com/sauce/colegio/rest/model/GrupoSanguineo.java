package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "grupos_sanguineos")
public class GrupoSanguineo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_grupo_san")
    private Long idGrupoSanguineo;

    @Column(name = "descricion")
    private String descripcion;

}
