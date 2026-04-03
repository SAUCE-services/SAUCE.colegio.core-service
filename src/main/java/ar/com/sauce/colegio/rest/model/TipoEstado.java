package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

@Data
@Entity
@Table(name = "tipos_estado")
public class TipoEstado extends Auditable implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado")
    private Long idFacturas;

    @Column(name = "descripcion")
    private String descripcion;
}
