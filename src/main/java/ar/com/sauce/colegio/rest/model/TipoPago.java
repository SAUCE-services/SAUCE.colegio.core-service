package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tipopago")
public class TipoPago {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tipo_id")
    private Long tipoId;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "auto_id")
    private Long autoId;

    @Column(name = "uid")
    private String uid;

}
