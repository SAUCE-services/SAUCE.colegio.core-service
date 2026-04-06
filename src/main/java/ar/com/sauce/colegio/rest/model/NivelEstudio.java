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
public class NivelEstudio extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idNivelEstudio;

    private String descripcion;

}
