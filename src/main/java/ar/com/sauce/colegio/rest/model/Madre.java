package ar.com.sauce.colegio.rest.model;

import com.sun.jdi.connect.Transport;
import jakarta.persistence.*;
import lombok.*;

import java.awt.*;
import java.io.Serializable;
import java.time.LocalDate;

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
    private Long idMadre;

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

    private String uuid;

    @ManyToOne
    @JoinColumn(name = "id_establecimiento")
    private Establecimiento establecimiento;

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
    @JoinColumn(name = "id_tipo_nacionalidad")
    private TipoNacionalidad tipoNacionalidad;

    @ManyToOne
    @JoinColumn(name = "id_tipo_doc")
    private TipoDocumento tipoDocumento;

    @ManyToOne
    @JoinColumn(name = "id_transporte")
    private Transporte transporte;

    @ManyToOne
    @JoinColumn(name = "id_parentesco")
    private Parentesco parentesco;

}
