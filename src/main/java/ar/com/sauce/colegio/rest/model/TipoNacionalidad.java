package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

@Data
@Entity
@Table(name = "tipo_nacionalidad")
public class TipoNacionalidad extends Auditable implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_nacionalidad")
    private Long idTipoNacionalidad;

    @Column(name = "descripcion")
    private String descripcion;
}
