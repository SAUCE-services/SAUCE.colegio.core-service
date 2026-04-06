package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "grupos_sanguineos")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GrupoSanguineo extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_grupo_san")
    private Long idGrupoSanguineo;

    private String descripcion;

}
