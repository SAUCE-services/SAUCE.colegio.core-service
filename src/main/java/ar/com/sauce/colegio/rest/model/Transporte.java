package ar.com.sauce.colegio.rest.model;

import jakarta.persistence.*;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;

@Data
@Entity
@Table(name = "transportes")
@AttributeOverride(name = "updated",
        column = @Column(name = "created", insertable = false, updatable = false))
public class Transporte extends Auditable implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_transporte")
    private Long idTransporte;

    @Column(name = "apellido")
    private String apellido;

    @Column(name = "nombre")
    private String nombre;

    @Column(name = "nro_documento")
    private Long nroDocumento;

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

    @ManyToOne
    @JoinColumn(name = "id_tipo_doc")
    private TipoDocumento tipoDocumento;

    @ManyToOne
    @JoinColumn(name = "id_localidad")
    private Localidad localidad;
}
