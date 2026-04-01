package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "o_sociales")
public class ObraSocial {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_osocial")
    private Long idObraSociañ;

    @Column(name = "descripcion")
    private String descripcion;
}
