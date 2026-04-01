package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "localidades")
public class Localidad {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_localidad")
    private Long idLocalidad;

    @Column(name = "descripcion")
    private String descripcion;

    // RELACIONES
    @ManyToOne
    @JoinColumn(name = "id_departamento")
    private Departamento departamento;
}
