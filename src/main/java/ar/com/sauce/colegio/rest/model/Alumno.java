package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.*;


import java.io.Serializable;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "alumnos")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Alumno extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idAlumno;

    private String apellido;

    private String nombre;

    private Long nroDocumento;

    private LocalDate fechaNacimiento;

    private LocalDate fechaIngreso;

    private String curso;

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
