package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

@Data
@Entity
@Table(name = "provincias")
public class Provincia extends Auditable implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_provincia")
    private Long idProvincia;

    @Column(name = "descricion")
    private String descripcion;

}
