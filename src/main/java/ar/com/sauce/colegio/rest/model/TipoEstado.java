package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "tipos_estado")
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class TipoEstado extends Auditable implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado")
    private Long estadoId;

    private String descripcion;

}
