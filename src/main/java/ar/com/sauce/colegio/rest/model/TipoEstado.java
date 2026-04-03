package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "tipos_estado")
public class TipoEstado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado")
    private Long idFacturas;

    @Column(name = "descripcion")
    private String descripcion;
}
