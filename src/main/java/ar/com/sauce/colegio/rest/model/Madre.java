package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "madres")
@Builder
@NoArgsConstructor
@AllArgsConstructor

public class Madre extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_madre")
    private Long madreId;

    private String apellido;

    private String nombre;

    private Long nroDocumento;

    private String dirCalle;

    @Column(name = "dir_num")
    private String dirNumero;

    private String dirPiso;

    private String dirDepto;

    @Column(name = "tel_fijo")
    private String telefonoFijo;

    @Column(name = "tel_cel")
    private String telefonoCelular;

    private Integer presente;

    @ManyToOne
    @JoinColumn(name = "id_departamento")
    private Departamento departamento;

    @ManyToOne
    @JoinColumn(name = "id_localidad")
    private Localidad localidad;

    @ManyToOne
    @JoinColumn(name = "id_actividad")
    private Actividad actividad;

    @ManyToOne
    @JoinColumn(name = "id_nivel_estudio")
    private NivelEstudio nivelEstudio;

    @ManyToOne
    @JoinColumn(name = "id_alumno")
    private Alumno alumno;

    @ManyToOne
    @JoinColumn(name = "id_tipo_doc")
    private TipoDocumento tipoDocumento;

    @ManyToOne
    @JoinColumn(name = "id_parentesco")
    private Parentesco parentesco;

}
