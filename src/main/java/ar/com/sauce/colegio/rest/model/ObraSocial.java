package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "o_sociales")
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class ObraSocial extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_osocial")
    private Long obraSocialId;

    private String descripcion;

}
