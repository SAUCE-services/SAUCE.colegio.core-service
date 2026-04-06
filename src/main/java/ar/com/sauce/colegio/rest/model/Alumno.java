package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.Data;


import java.io.Serializable;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "alumnos")
public class Alumno extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_alumno")
    private Long idAlumno;

    @Column(name = "apellido")
    private String apellido;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "nro_documento")
    private Long nroDocumento;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "fecha_ingreso")
    private LocalDate fechaIngreso;

    @Column(name = "curso")
    private String curso;

    @Column(name = "uuid")
    private String uuid;

    @ManyToOne
    @JoinColumn(name = "id_establecimiento")
    private Establecimiento establecimiento;

    @ManyToOne
    @JoinColumn(name = "id_tipo_nacionalidad")
    private TipoNacionalidad tipoNacionalidad;

    @ManyToOne
    @JoinColumn(name = "id_tipo_doc")
    private TipoDocumento tipoDocumento;

    @ManyToOne
    @JoinColumn(name = "id_transporte")
    private Transporte transporte;

}
