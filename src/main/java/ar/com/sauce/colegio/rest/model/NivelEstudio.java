package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "niveles_estudio")
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class NivelEstudio extends AuditableSimple implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_nivel_estudio")
    private Long nivelEstudioId;

    private String descripcion;

}
