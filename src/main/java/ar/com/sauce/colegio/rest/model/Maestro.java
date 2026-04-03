package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;

@Data
@Entity
@Table(name = "maestros")
public class Maestro extends Auditable implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_maestro")
    private Long idMaestro;

    @Column(name = "apellido")
    private String apellido;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "id_tipo_doc")
    private Long idTipoDoc;

    @Column(name = "nro_documento")
    private String nroDocumento;

    @Column(name = "dir_calle")
    private String dirCalle;

    @Column(name = "dir_num")
    private String dirNumero;

    @Column(name = "dir_piso")
    private String dirPiso;

    @Column(name = "dir_depto")
    private String dirDepto;

    @Column(name = "tel_fijo")
    private String telefonoFijo;

    @Column(name = "tel_cel")
    private String telefonoCelular;

    // RELACIONES
    @ManyToOne
    @JoinColumn(name = "id_localidad")
    private Localidad localidad;

    @ManyToOne
    @JoinColumn(name = "id_actividad")
    private Actividad actividad;

    @ManyToOne
    @JoinColumn(name = "id_establecimiento")
    private Establecimiento establecimiento;

}
