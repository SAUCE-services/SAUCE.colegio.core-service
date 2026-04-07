package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "departamentos")
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Departamento extends AuditableSimple implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_departamento")
    private Long departamentoId;

    @Column(name = "cod_postal")
    private String codigoPostal;

    private String descripcion;

    @ManyToOne
    @JoinColumn(name = "id_provincia")
    private Provincia provincia;

}
